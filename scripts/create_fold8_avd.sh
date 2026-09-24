#!/usr/bin/env bash
# Creates "Galaxy_Z_Fold_8": a foldable emulator with Galaxy Z Fold 8 screen specs
#   main (unfolded) 2448x1848 (4:3), cover (folded) 1248x1972 (10:16), 420dpi, working fold/unfold/tabletop.
# The emulator's fold mechanics only work on Google's foldable panel geometry, so we boot that
# geometry and override each panel's resolution with `wm size` (overrides persist per panel).
# Note: this is stock Android with Fold 8 dimensions, not Samsung One UI.
set -euo pipefail
export ANDROID_HOME="${ANDROID_HOME:-/opt/homebrew/share/android-commandlinetools}"
export JAVA_HOME="${JAVA_HOME:-/opt/homebrew/opt/openjdk@21}"
ADB="$ANDROID_HOME/platform-tools/adb"
IMAGE="system-images;android-37.0;google_apis;arm64-v8a"
NAME=Galaxy_Z_Fold_8

echo no | "$ANDROID_HOME/cmdline-tools/latest/bin/avdmanager" create avd -n "$NAME" -k "$IMAGE" -d pixel_9_pro_fold --force >/dev/null 2>&1 \
  || echo no | avdmanager create avd -n "$NAME" -k "$IMAGE" -d pixel_9_pro_fold --force >/dev/null
CFG="$HOME/.android/avd/$NAME.avd/config.ini"
sed -i '' 's/^avd.ini.displayname=.*/avd.ini.displayname=Galaxy Z Fold 8 (spec clone)/; s/^showDeviceFrame=.*/showDeviceFrame=no/' "$CFG"
grep -q '^avd.ini.displayname=' "$CFG" || echo 'avd.ini.displayname=Galaxy Z Fold 8 (spec clone)' >> "$CFG"

"$ANDROID_HOME/emulator/emulator" -avd "$NAME" -no-audio -no-boot-anim -gpu host >/tmp/emulator-$NAME.log 2>&1 &
"$ADB" wait-for-device
until [ "$("$ADB" shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" = "1" ]; do sleep 3; done
sleep 5

# Keep apps running on the cover screen when folding (like One UI), no lock screen.
"$ADB" shell settings put secure fold_lock_behavior_setting stay_awake_on_fold_key
"$ADB" shell locksettings set-disabled true >/dev/null
"$ADB" shell settings put global stay_on_while_plugged_in 7

# Per-panel Fold 8 resolutions.
"$ADB" emu fold >/dev/null; sleep 3; "$ADB" shell input keyevent KEYCODE_WAKEUP
"$ADB" shell wm size 1248x1972; "$ADB" shell wm density 420
"$ADB" emu unfold >/dev/null; sleep 3; "$ADB" shell input keyevent KEYCODE_WAKEUP
"$ADB" shell wm size 2448x1848; "$ADB" shell wm density 420
echo "Ready: unfolded $("$ADB" shell wm size | tail -1)"
echo "Fold/unfold: adb emu fold | adb emu unfold · Tabletop: adb emu posture 2"
