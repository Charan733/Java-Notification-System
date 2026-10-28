#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

rm -rf build/classes build/web
mkdir -p build/classes build/web
find src/main/java -name '*.java' -print0 \
  | xargs -0 javac --release 17 --add-modules jdk.httpserver -encoding UTF-8 -d build/classes
cp -R src/main/resources/web/. build/web/

echo "Java classes and web assets built in $ROOT/build."
