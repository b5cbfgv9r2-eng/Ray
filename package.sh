#!/usr/bin/env bash
set -euo pipefail
cd -- "$(dirname -- "$0")"
bash ./build.sh
mkdir -p build/package downloads
cp dist/ray-snake.jar build/package/ray-snake.jar
cp launchers/play.bat launchers/play.sh launchers/README.txt build/package/
jar --create --no-manifest --file downloads/ray-snake.zip -C build/package .
echo 'Packaged downloads/ray-snake.zip (Java 17+ required to play)'
