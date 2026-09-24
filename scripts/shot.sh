#!/usr/bin/env bash
# Usage: scripts/shot.sh <name>  — captures the display that is currently on to test-output/<name>.png
# Foldable emulators have two panels (inner/cover); only one is on at a time.
set -euo pipefail
ADB="${ANDROID_HOME:-/opt/homebrew/share/android-commandlinetools}/platform-tools/adb"
OUT="$(dirname "$0")/../test-output/$1.png"
mkdir -p "$(dirname "$OUT")"
"$ADB" shell input keyevent KEYCODE_WAKEUP
sleep 0.6
ID=$("$ADB" shell dumpsys display | python3 -c '
import re, sys
for line in sys.stdin:
    if "DisplayDeviceInfo{" in line and " state ON" in line:
        m = re.search(r"uniqueId=\"local:(\d+)\"", line)
        if m:
            print(m.group(1)); break
')
"$ADB" exec-out screencap -p ${ID:+-d "$ID"} > "$OUT"
echo "test-output/$1.png"
