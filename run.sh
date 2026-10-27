#!/usr/bin/env bash
set -euo pipefail
cd -- "$(dirname -- "$0")"
bash ./build.sh
exec java -jar dist/ray-snake.jar "$@"
