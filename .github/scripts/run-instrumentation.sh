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

if [[ "$status" -eq 0 && "${#results[@]}" -eq 0 ]]; then
  echo "::error::Instrumentation produced no XML test results"
  status=1
fi

if [[ "$status" -eq 0 ]] && ! grep -Eh '<testsuite[^>]*tests="[1-9][0-9]*"' "${results[@]}" >/dev/null 2>&1; then
  echo "::error::Instrumentation XML contains no executed tests"
  status=1
fi

if [[ "$status" -ne 0 ]]; then
  echo "::group::Instrumentation test XML results"
  find app/build -path "*androidTest-results*" -type f -name "*.xml" -print -exec cat {} \; || true
  echo "::endgroup::"
fi

exit "$status"
