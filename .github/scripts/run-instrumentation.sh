#!/usr/bin/env bash

set +e

instrumentation_log="connected-android-test.log"
instrumentation_logcat="android-instrumentation-logcat.log"
direct_only="${DIRECT_ANDROID_INSTRUMENTATION:-0}"
rm -f "$instrumentation_log" "$instrumentation_logcat"

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

run_direct_instrumentation() {
  local target_package="com.arttvad.worktime"
  local test_package="${target_package}.test"
  local app_apk
  local test_apk
  local app_install_status=0
  local test_install_status=0
  local app_was_installed=0
  local test_was_installed=0
  local instrumentation_line
  local component
  local direct_status
  local broadcast_wait_status

  app_apk="$(find app/build/outputs/apk/debug -maxdepth 1 -type f -name '*.apk' | head -n 1)"
  test_apk="$(find app/build/outputs/apk/androidTest/debug -maxdepth 1 -type f -name '*.apk' | head -n 1)"

  if [[ -z "$app_apk" || -z "$test_apk" ]]; then
    echo "::error::Could not locate app and test APKs for direct instrumentation" | tee -a "$instrumentation_log"
    return 1
  fi

  # Keep first-boot Play Store/GMS work out of the short package-install window.
  for package_name in \
    com.android.vending \
    com.google.android.apps.photos \
    com.google.android.apps.wellbeing; do
    adb shell am force-stop "$package_name" >/dev/null 2>&1 || true
  done
  adb shell am force-stop com.google.android.gms >/dev/null 2>&1 || true

  if adb shell pm path "$target_package" >/dev/null 2>&1; then
    echo "Reusing installed target APK." | tee -a "$instrumentation_log"
    app_was_installed=1
  else
    adb install -r -t "$app_apk" 2>&1 | tee -a "$instrumentation_log"
    app_install_status=${PIPESTATUS[0]}
  fi

  if adb shell pm path "$test_package" >/dev/null 2>&1; then
    echo "Reusing installed test APK." | tee -a "$instrumentation_log"
    test_was_installed=1
  else
    adb install -r -t "$test_apk" 2>&1 | tee -a "$instrumentation_log"
    test_install_status=${PIPESTATUS[0]}
  fi

  if [[ "$app_install_status" -ne 0 || "$test_install_status" -ne 0 ]]; then
    echo "::error::Direct instrumentation APK installation failed" | tee -a "$instrumentation_log"
    return 1
  fi

  # A reused UTP installation may contain state from a failed zero-test attempt.
  # Fresh direct-only installations need no reset and therefore avoid two extra
  # PACKAGE_DATA_CLEARED broadcasts.
  if [[ "$app_was_installed" -eq 1 ]]; then
    adb shell pm clear "$target_package" >/dev/null 2>&1 || true
  fi
  if [[ "$test_was_installed" -eq 1 ]]; then
    adb shell pm clear "$test_package" >/dev/null 2>&1 || true
  fi

  instrumentation_line="$(adb shell pm list instrumentation 2>&1 | tr -d '\r' | tee -a "$instrumentation_log" | grep "(target=${target_package})" | head -n 1)"
  component="${instrumentation_line#instrumentation:}"
  component="${component%% *}"

  if [[ -z "$component" || "$component" == "$instrumentation_line" ]]; then
    echo "::error::AndroidJUnitRunner instrumentation component is not registered" | tee -a "$instrumentation_log"
    return 1
  fi

  echo "::group::Direct instrumentation memory preparation" | tee -a "$instrumentation_log"
  adb shell cat /proc/meminfo 2>&1 \
    | grep -E '^(MemTotal|MemAvailable|Cached):' \
    | tee -a "$instrumentation_log" || true

  # Package install broadcasts can wake Play Store/GMS observers even when the
  # compatibility suite itself is offline. Stop irrelevant consumers again,
  # then wait for the framework queues to drain before starting the test process.
  for package_name in \
    com.android.vending \
    com.google.android.apps.photos \
    com.google.android.apps.wellbeing; do
    adb shell am force-stop "$package_name" >/dev/null 2>&1 || true
  done
  adb shell am force-stop com.google.android.gms >/dev/null 2>&1 || true

  timeout 60s adb shell am wait-for-broadcast-idle --flush-broadcast-loopers 2>&1 \
    | tee -a "$instrumentation_log"
  broadcast_wait_status=${PIPESTATUS[0]}
  if [[ "$broadcast_wait_status" -ne 0 ]]; then
    echo "::warning::Broadcast-idle wait did not complete within 60 seconds; continuing with cached-process reclaim" \
      | tee -a "$instrumentation_log"
  fi

  adb shell am kill-all >/dev/null 2>&1 || true
  adb shell am force-stop com.google.android.gms >/dev/null 2>&1 || true
  sleep 5

  adb shell cat /proc/meminfo 2>&1 \
    | grep -E '^(MemTotal|MemAvailable|Cached):' \
    | tee -a "$instrumentation_log" || true
  echo "::endgroup::" | tee -a "$instrumentation_log"

  adb logcat -c >/dev/null 2>&1 || true
  adb shell am instrument -w -r "$component" 2>&1 | tee -a "$instrumentation_log"
  direct_status=${PIPESTATUS[0]}

  if [[ "$direct_status" -ne 0 ]] \
    || grep -Eq 'FAILURES!!!|INSTRUMENTATION_FAILED|Process crashed' "$instrumentation_log" \
    || ! grep -Eq 'OK \([1-9][0-9]* tests?\)' "$instrumentation_log"; then
    adb logcat -d -v threadtime > "$instrumentation_logcat" 2>&1 || true
    echo "::group::Direct instrumentation crash lines" | tee -a "$instrumentation_log"
    grep -E 'AndroidRuntime|FATAL EXCEPTION|Fatal signal|DEBUG|crash_dump|AndroidJUnitRunner|TestRunner|lowmemorykiller|lmkd|com\.arttvad\.worktime|Process: com\.arttvad\.worktime' "$instrumentation_logcat" \
      | tail -n 300 \
      | tee -a "$instrumentation_log" || true
    echo "::endgroup::" | tee -a "$instrumentation_log"
    echo "::error::Direct Android instrumentation did not complete a non-empty passing suite" | tee -a "$instrumentation_log"
    return 1
  fi

  echo "Direct adb instrumentation completed a non-empty passing suite." | tee -a "$instrumentation_log"
  return 0
}

status=0
xml_has_tests=0

if [[ "$direct_only" == "1" ]]; then
  echo "Android direct-instrumentation mode: building APKs without AGP/UTP device execution." | tee "$instrumentation_log"
  ./gradlew assembleDebug assembleDebugAndroidTest --stacktrace 2>&1 | tee -a "$instrumentation_log"
  status=${PIPESTATUS[0]}

  if [[ "$status" -eq 0 ]]; then
    run_direct_instrumentation
    status=$?
  fi
else
  ./gradlew connectedDebugAndroidTest --stacktrace 2>&1 | tee "$instrumentation_log"
  status=${PIPESTATUS[0]}

  if grep -q "AndroidTestRunner failed" "$instrumentation_log"; then
    echo "::error::Android test runner failed even though Gradle returned success"
    status=1
  fi

  mapfile -t results < <(find app/build -path "*androidTest-results*" -type f -name "*.xml")
  if [[ "${#results[@]}" -gt 0 ]] \
    && grep -Eh '<testsuite[^>]*tests="[1-9][0-9]*"' "${results[@]}" >/dev/null 2>&1; then
    xml_has_tests=1
  fi

  # Keep the fallback for unexpected future AGP/runner zero-test behavior on
  # non-Android-17 jobs, but never accept a zero-test Gradle success as green.
  if [[ "$status" -eq 0 && "$xml_has_tests" -ne 1 ]]; then
    echo "::warning::Gradle instrumentation reported zero executed tests; retrying with direct adb instrumentation" \
      | tee -a "$instrumentation_log"
    run_direct_instrumentation
    status=$?
  fi
fi

if [[ "$status" -eq 0 && "$direct_only" != "1" && "$xml_has_tests" -ne 1 ]] \
  && ! grep -Eq 'OK \([1-9][0-9]* tests?\)' "$instrumentation_log"; then
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
