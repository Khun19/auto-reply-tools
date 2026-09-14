#!/usr/bin/env bash
set -u

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
GRADLEW="$ROOT_DIR/android/gradlew"

if [ -z "${ANDROID_HOME:-}" ] || [ ! -d "${ANDROID_HOME:-}" ]; then
  echo "Android SDK is unavailable; skipped assembleDebug."
  exit 0
fi

if [ ! -x "$GRADLEW" ]; then
  chmod +x "$GRADLEW"
fi

cd "$ROOT_DIR/android"
exec "$GRADLEW" :app:assembleDebug --no-daemon
