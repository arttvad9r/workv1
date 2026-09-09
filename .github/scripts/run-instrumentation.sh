#!/usr/bin/env bash

set +e

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
    adb uninstall "$test_package" >/dev/null 2>&1 || true
    adb uninstall "$target_package" >/dev/null 2>&1 || true

    adb install -r -t "$app_apk" 2>&1 | tee -a connected-android-test.log
    app_install_status=${PIPESTATUS[0]}
    adb install -r -t "$test_apk" 2>&1 | tee -a connected-android-test.log
    test_install_status=${PIPESTATUS[0]}

    if [[ "$app_install_status" -ne 0 || "$test_install_status" -ne 0 ]]; then
      echo "::error::Direct instrumentation APK installation failed" | tee -a connected-android-test.log
      status=1
    else
      instrumentation_line="$(adb shell pm list instrumentation 2>&1 | tr -d '\r' | tee -a connected-android-test.log | grep "(target=${target_package})" | head -n 1)"
      component="${instrumentation_line#instrumentation:}"
      component="${component%% *}"

      if [[ -z "$component" || "$component" == "$instrumentation_line" ]]; then
        echo "::error::AndroidJUnitRunner instrumentation component is not registered" | tee -a connected-android-test.log
        status=1
      else
        adb shell am instrument -w -r "$component" 2>&1 | tee -a connected-android-test.log
        direct_status=${PIPESTATUS[0]}

        if [[ "$direct_status" -ne 0 ]] \
          || grep -Eq 'FAILURES!!!|INSTRUMENTATION_FAILED|Process crashed' connected-android-test.log \
          || ! grep -Eq 'OK \([1-9][0-9]* tests?\)' connected-android-test.log; then
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
  adb logcat -d -v threadtime | tail -n 500 || true
  echo "::endgroup::"
fi

exit "$status"
