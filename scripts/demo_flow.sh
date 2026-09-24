#!/usr/bin/env bash
# Usage: demo_flow.sh [N] [purpose title]
# Launches the app fresh, taps a purpose card on the home screen, then picks the first N photos.
set -euo pipefail
ADB="${ANDROID_HOME:-/opt/homebrew/share/android-commandlinetools}/platform-tools/adb"
cd "$(dirname "$0")/.."
N="${1:-6}"
PURPOSE="${2:-메인 화면 배경화면}"
"$ADB" shell am force-stop app.wireframephoto.debug
"$ADB" shell am start -n app.wireframephoto.debug/app.wireframephoto.ui.MainActivity >/dev/null
sleep 2
scripts/ui.py tap-scroll "$PURPOSE" >/dev/null
sleep 3
python3 - "$N" <<'PY'
import subprocess, sys, re, time
n = int(sys.argv[1])
out = subprocess.run(["scripts/ui.py", "dump"], capture_output=True, text=True).stdout
pts = [tuple(map(int, m)) for m in re.findall(r"\((\d+), (\d+)\) 'Photo taken", out)]
for x, y in pts[:n]:
    subprocess.run(["scripts/ui.py", "tap", str(x), str(y)])
    time.sleep(0.4)
PY
scripts/ui.py tap-text "Done" >/dev/null
sleep 3
