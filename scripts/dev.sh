#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
bash "$ROOT/scripts/build.sh"
exec java --add-modules jdk.httpserver \
  -Dnotification.webRoot="$ROOT/src/main/resources/web" \
  -cp "$ROOT/build/classes" \
  com.acme.notifications.Main --server
