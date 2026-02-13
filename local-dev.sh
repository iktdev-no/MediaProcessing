#!/usr/bin/env bash
set -e

APP="$1"

if [ -z "$APP" ]; then
  echo "Usage: ./local-dev.sh <app-name>"
  exit 1
fi

ROOT_DIR="$(cd "$(dirname "$0")" && pwd)"
GRADLEW="$ROOT_DIR/gradlew"

FRONTEND_DIR="$ROOT_DIR/apps/$APP/web"
BACKEND_DIR="$ROOT_DIR/apps/$APP"
STATIC_DIR="$BACKEND_DIR/src/main/resources/static"

echo ""
echo "=== Building frontend for Spring Boot fallback test ==="
echo ""

if [ -d "$FRONTEND_DIR" ]; then
  (
    cd "$FRONTEND_DIR"
    npm install --silent
    npm run build
  )

  echo "Copying dist → static..."
  rm -rf "$STATIC_DIR"
  mkdir -p "$STATIC_DIR"
  cp -r "$FRONTEND_DIR/dist/"* "$STATIC_DIR/"
else
  echo "No frontend found for $APP"
fi

echo ""
echo "=== Starting backend (Spring Boot devtools) ==="
echo ""

(
  cd "$BACKEND_DIR"
  "$GRADLEW" bootRun --quiet
) &
BACKEND_PID=$!

echo ""
echo "=== Starting Vite dev server (hot reload) ==="
echo ""

(
  cd "$FRONTEND_DIR"
  npm run dev
) &
FRONTEND_PID=$!

trap "echo ''; echo 'Stopping dev environment...'; kill $FRONTEND_PID $BACKEND_PID 2>/dev/null" EXIT

wait
