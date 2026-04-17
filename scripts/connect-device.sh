#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ANDROID_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
ROOT_DIR="$(cd "$ANDROID_DIR/.." && pwd)"
BACKEND_DIR="$ROOT_DIR/vormex-backend"
ADB_SERIAL="${1:-}"

adb_cmd=(adb)
if [[ -n "$ADB_SERIAL" ]]; then
  adb_cmd+=(-s "$ADB_SERIAL")
fi

if ! "${adb_cmd[@]}" get-state >/dev/null 2>&1; then
  echo "No Android device detected via adb."
  echo "Check: adb devices -l"
  exit 1
fi

if ! curl -fsS "http://127.0.0.1:5000/api/health" >/dev/null 2>&1; then
  cat <<EOF
Backend is not responding on http://127.0.0.1:5000.
Start it in another terminal with:
  cd "$BACKEND_DIR"
  npm run dev
EOF
  exit 1
fi

echo "Reversing device port 5000 to local port 5000..."
"${adb_cmd[@]}" reverse tcp:5000 tcp:5000

echo "Installing debug app on connected device..."
(cd "$ANDROID_DIR" && ./gradlew :catalog:installDebug)

echo "Launching app..."
"${adb_cmd[@]}" shell am start -n com.vormex.android/com.kyant.backdrop.catalog.MainActivity >/dev/null

echo "Vormex is installed and connected to the local backend on port 5000."
