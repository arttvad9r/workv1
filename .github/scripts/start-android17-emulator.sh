#!/usr/bin/env bash

set -euo pipefail

avd_name="worktime-api37"
system_image="system-images;android-37.1;google_apis_playstore_ps16k;x86_64"
emulator_log="android17-emulator.log"
emulator_bin="$ANDROID_SDK_ROOT/emulator/emulator"

sdkmanager "emulator" "$system_image"
echo no | avdmanager create avd \
  --force \
  --name "$avd_name" \
  --package "$system_image" \
  --device "pixel_7"

nohup "$emulator_bin" \
  -avd "$avd_name" \
  -no-window \
  -gpu swiftshader_indirect \
  -no-snapshot \
  -noaudio \
  -no-boot-anim \
  >"$emulator_log" 2>&1 &
echo $! > android17-emulator.pid

ready=0
for attempt in $(seq 1 120); do
  adb_state="$(adb get-state 2>/dev/null || true)"
  if [[ "$adb_state" == "device" ]]; then
    boot_completed="$(adb shell getprop sys.boot_completed 2>/dev/null | tr -d '\r' || true)"
    if [[ "$boot_completed" == "1" ]] && adb shell cmd package path android >/dev/null 2>&1; then
      ready=1
      break
    fi
  fi
  sleep 2
done

if [[ "$ready" -ne 1 ]]; then
  echo "::error::Android 17 emulator did not reach ADB + boot + Package Manager readiness"
  adb devices -l || true
  cat "$emulator_log" || true
  exit 1
fi

# Unlock only after framework services are actually ready. The third-party
# emulator action currently performs this immediately after sys.boot_completed,
# which is too early for the Android 17 16 KB Play Store image.
adb shell input keyevent 82 || true
adb shell settings put global window_animation_scale 0.0
adb shell settings put global transition_animation_scale 0.0
adb shell settings put global animator_duration_scale 0.0
