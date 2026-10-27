#!/usr/bin/env bash
set -euo pipefail
cd -- "$(dirname -- "$0")"
if ! command -v javac >/dev/null || ! command -v jar >/dev/null; then
  echo 'Please install JDK 17 or newer and add its bin directory to PATH.' >&2
  exit 1
fi
mkdir -p build/classes dist
javac --release 17 -encoding UTF-8 -Xlint:all -d build/classes src/main/java/ray/snake/*.java
jar --create --file dist/ray-snake.jar --main-class ray.snake.SnakeApp -C build/classes .
echo 'Built dist/ray-snake.jar'
