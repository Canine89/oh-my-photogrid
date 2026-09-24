#!/usr/bin/env bash
# Runs unit tests, builds the signed release APK, verifies the signature, and copies it to dist/.
set -euo pipefail
cd "$(dirname "$0")/.."
export JAVA_HOME="${JAVA_HOME:-/opt/homebrew/opt/openjdk@21}"
export ANDROID_HOME="${ANDROID_HOME:-/opt/homebrew/share/android-commandlinetools}"
[ -f keystore.properties ] || { echo "keystore.properties missing — see docs/04-release.md"; exit 1; }

./gradlew --console=plain -q :app:testDebugUnitTest :app:assembleRelease

APK=app/build/outputs/apk/release/app-release.apk
VERSION=$(grep -m1 'versionName' app/build.gradle.kts | sed -E 's/.*"(.*)".*/\1/')
BT=$(ls -d "$ANDROID_HOME"/build-tools/* | sort -V | tail -1)
"$BT/apksigner" verify --min-sdk-version 29 "$APK"

mkdir -p dist
OUT="dist/WireframePhoto-$VERSION.apk"
cp "$APK" "$OUT"
shasum -a 256 "$OUT" | tee "$OUT.sha256"
echo "Built $OUT ($(du -h "$OUT" | cut -f1))"
