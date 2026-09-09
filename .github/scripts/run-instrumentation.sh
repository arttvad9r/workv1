#!/usr/bin/env bash

set +e

instrumentation_logcat="android-instrumentation-logcat.log"
rm -f "$instrumentation_logcat"

package_ready=0
for attempt in $(seq 1 60); do
  if adb shell cmd package path android >/dev/null 2>&1; then
    package_ready=1
    break
  fi
  sleep 2
done

if [[ "$package_ready" -ne 1 ]]; then
  echo "::error::Android package manager did not become ready"
  exit 1
fi

./gradlew connectedDebugAndroidTest --stacktrace 2>&1 | tee connected-android-test.log
status=${PIPESTATUS[0]}

if grep -q "AndroidTestRunner failed" connected-android-test.log; then
  echo "::error::Android test runner failed even though Gradle returned success"
  status=1
fi

mapfile -t results < <(find app/build -path "*androidTest-results*" -type f -name "*.xml")
xml_has_tests=0
if [[ "${#results[@]}" -gt 0 ]] && grep -Eh '<testsuite[^>]*tests="[1-9][0-9]*"' "${results[@]}" >/dev/null 2>&1; then
  xml_has_tests=1
fi

# Android 17 API 37.1 Play Store 16 KB images can currently make AGP/UTP
# return BUILD SUCCESSFUL with a zero-test XML. In that case, verify the same
# built APKs with Android's instrumentation command directly instead of
# accepting a false-green result.
if [[ "$status" -eq 0 && "$xml_has_tests" -ne 1 ]]; then
  echo "::warning::Gradle instrumentation reported zero executed tests; retrying with direct adb instrumentation" | tee -a connected-android-test.log

  target_package="com.arttvad.worktime"
  test_package="${target_package}.test"
  app_apk="$(find app/build/outputs/apk/debug -maxdepth 1 -type f -name '*.apk' | head -n 1)"
  test_apk="$(find app/build/outputs/apk/androidTest/debug -maxdepth 1 -type f -name '*.apk' | head -n 1)"

  if [[ -z "$app_apk" || -z "$test_apk" ]]; then
    echo "::error::Could not locate app and test APKs for direct instrumentation" | tee -a connected-android-test.log
    status=1
  else
    app_install_status=0
    test_install_status=0

    # UTP normally leaves the just-built APKs installed even when it reports a
    # zero-test XML. Reuse those exact installs when possible. Uninstalling and
    # reinstalling both packages triggered Play Store/GMS package scans on the
    # API 37.1 Play Store image, creating enough transient low-memory pressure
    # for LMKD to kill the instrumentation process before the first test.
    if adb shell pm path "$target_package" >/dev/null 2>&1; then
      echo "Reusing UTP-installed target APK." | tee -a connected-android-test.log
    else
      adb install -r -t "$app_apk" 2>&1 | tee -a connected-android-test.log
      app_install_status=${PIPESTATUS[0]}
    fi

    if adb shell pm path "$test_package" >/dev/null 2>&1; then
      echo "Reusing UTP-installed test APK." | tee -a connected-android-test.log
    else
      adb install -r -t "$test_apk" 2>&1 | tee -a connected-android-test.log
      test_install_status=${PIPESTATUS[0]}
    fi

    if [[ "$app_install_status" -ne 0 || "$test_install_status" -ne 0 ]]; then
      echo "::error::Direct instrumentation APK installation failed" | tee -a connected-android-test.log
      status=1
    else
      # Clear data without package removal so the fallback starts from a clean
      # app/test state without producing PACKAGE_REMOVED/PACKAGE_ADDED churn.
      adb shell pm clear "$test_package" >/dev/null 2>&1 || true
      adb shell pm clear "$target_package" >/dev/null 2>&1 || true

      instrumentation_line="$(adb shell pm list instrumentation 2>&1 | tr -d '\r' | tee -a connected-android-test.log | grep "(target=${target_package})" | head -n 1)"
      component="${instrumentation_line#instrumentation:}"
      component="${component%% *}"

      if [[ -z "$component" || "$component" == "$instrumentation_line" ]]; then
        echo "::error::AndroidJUnitRunner instrumentation component is not registered" | tee -a connected-android-test.log
        status=1
      else
        # Fresh installs on the Android 17 Play Store image trigger package and
        # Play Store/GMS background work. Wait for broadcast delivery to settle,
        # then reclaim cached processes before starting the test process. This
        # avoids a pre-test LMKD kill under transient guest-memory pressure while
        # keeping the test assertions and strict non-empty-suite gate unchanged.
        echo "::group::Direct instrumentation memory preparation" | tee -a connected-android-test.log
        adb shell cat /proc/meminfo 2>&1 | grep -E '^(MemTotal|MemAvailable|Cached):' | tee -a connected-android-test.log || true
        timeout 30s adb shell am wait-for-broadcast-idle --flush-broadcast-loopers 2>&1 | tee -a connected-android-test.log
        broadcast_wait_status=${PIPESTATUS[0]}
        if [[ "$broadcast_wait_status" -ne 0 ]]; then
          echo "::warning::Broadcast-idle wait did not complete within 30 seconds; continuing with cached-process reclaim" | tee -a connected-android-test.log
        fi
        adb shell am kill-all 2>&1 | tee -a connected-android-test.log || true
        sleep 2
        adb shell cat /proc/meminfo 2>&1 | grep -E '^(MemTotal|MemAvailable|Cached):' | tee -a connected-android-test.log || true
        echo "::endgroup::" | tee -a connected-android-test.log

        # Keep a clean device log around the direct runner. The Android 17 Play
        # Store image produces substantial unrelated boot/package noise, which
        # previously hid an immediate instrumentation-process crash.
        adb logcat -c >/dev/null 2>&1 || true
        adb shell am instrument -w -r "$component" 2>&1 | tee -a connected-android-test.log
        direct_status=${PIPESTATUS[0]}

        if [[ "$direct_status" -ne 0 ]] \
          || grep -Eq 'FAILURES!!!|INSTRUMENTATION_FAILED|Process crashed' connected-android-test.log \
          || ! grep -Eq 'OK \([1-9][0-9]* tests?\)' connected-android-test.log; then
          adb logcat -d -v threadtime > "$instrumentation_logcat" 2>&1 || true
          echo "::group::Direct instrumentation crash lines" | tee -a connected-android-test.log
          grep -E 'AndroidRuntime|FATAL EXCEPTION|Fatal signal|DEBUG|crash_dump|AndroidJUnitRunner|TestRunner|lowmemorykiller|lmkd|com\.arttvad\.worktime|Process: com\.arttvad\.worktime' "$instrumentation_logcat" \
            | tail -n 300 \
            | tee -a connected-android-test.log || true
          echo "::endgroup::" | tee -a connected-android-test.log
          echo "::error::Direct Android instrumentation did not complete a non-empty passing suite" | tee -a connected-android-test.log
          status=1
        else
          echo "Direct adb instrumentation completed a non-empty passing suite." | tee -a connected-android-test.log
          status=0
        fi
      fi
    fi
  fi
fi

if [[ "$status" -eq 0 && "$xml_has_tests" -ne 1 ]] && ! grep -Eq 'OK \([1-9][0-9]* tests?\)' connected-android-test.log; then
  echo "::error::Instrumentation produced neither non-empty XML nor a non-empty direct runner result"
  status=1
fi

if [[ "$status" -ne 0 ]]; then
  echo "::group::Instrumentation test XML results"
  find app/build -path "*androidTest-results*" -type f -name "*.xml" -print -exec cat {} \; || true
  echo "::endgroup::"
  echo "::group::Device instrumentation diagnostics"
  adb shell pm list instrumentation || true
  if [[ -f "$instrumentation_logcat" ]]; then
    tail -n 500 "$instrumentation_logcat" || true
  else
    adb logcat -d -v threadtime | tail -n 500 || true
  fi
  echo "::endgroup::"
fi

exit "$status"
