#!/usr/bin/env bash

set -euo pipefail

avd_name="worktime-api37"
system_image="system-images;android-37.1;google_apis_playstore_ps16k;x86_64"
emulator_log="android17-emulator.log"
emulator_bin="$ANDROID_SDK_ROOT/emulator/emulator"
avd_home="${RUNNER_TEMP:-$PWD/.android-avd}"
avd_path="$avd_home/$avd_name.avd"
adb_key="$HOME/.android/adbkey"

export ANDROID_AVD_HOME="$avd_home"
mkdir -p "$ANDROID_AVD_HOME" "$HOME/.android"

sdkmanager "emulator" "$system_image"
echo no | avdmanager create avd \
  --force \
  --name "$avd_name" \
  --package "$system_image" \
  --device "pixel_7" \
  --path "$avd_path"

# Play Store images enforce ADB authentication. Create the host key before the
# emulator starts so it can receive the public key during boot instead of
# entering the unauthorized state.
if [[ ! -f "$adb_key" ]]; then
  adb keygen "$adb_key"
fi
chmod 600 "$adb_key"
adb start-server >/dev/null

# Fail early with useful diagnostics if avdmanager and emulator disagree about
# where the AVD was created.
if ! "$emulator_bin" -list-avds | grep -Fxq "$avd_name"; then
  echo "::error::Android 17 AVD was created but is not visible to emulator"
  find "$ANDROID_AVD_HOME" -maxdepth 2 -type f -print || true
  exit 1
fi

nohup "$emulator_bin" \
  -avd "$avd_name" \
  -memory 3072 \
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

# The only currently usable API 37.1 16 KB x86_64 image is Play Store based.
# Its first-boot Play Store/GMS provisioning can create severe zone-level memory
# pressure even when /proc/meminfo reports gigabytes available. Compatibility
# tests are intentionally offline and do not exercise Google consumer apps, so
# quiesce that unrelated activity before installing/running the app under test.
if ! adb shell cmd connectivity airplane-mode enable >/dev/null 2>&1; then
  adb shell settings put global airplane_mode_on 1 || true
  adb shell am broadcast -a android.intent.action.AIRPLANE_MODE --ez state true >/dev/null 2>&1 || true
fi

for package_name in \
  com.android.vending \
  com.google.android.apps.photos \
  com.google.android.apps.wellbeing; do
  if adb shell pm path "$package_name" >/dev/null 2>&1; then
    adb shell pm disable-user --user 0 "$package_name" >/dev/null 2>&1 || true
    adb shell am force-stop "$package_name" >/dev/null 2>&1 || true
  fi
done

# GMS is part of the system image and may be needed by framework components, so
# do not disable it. Force-stop only its current first-boot work and allow the
# system to restart whatever it actually requires.
adb shell am force-stop com.google.android.gms >/dev/null 2>&1 || true

if ! timeout 45s adb shell am wait-for-broadcast-idle --flush-broadcast-loopers; then
  echo "::warning::Android 17 first-boot broadcasts did not fully settle within 45 seconds"
fi
adb shell am kill-all >/dev/null 2>&1 || true
sleep 3
