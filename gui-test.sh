#!/usr/bin/env bash
set -euo pipefail
cd -- "$(dirname -- "$0")"
if [[ -z "${DISPLAY:-}" ]]; then
  echo 'A desktop display or Xvfb is required for the GUI test.' >&2
  exit 1
fi
bash ./test.sh
exec java -cp build/classes:build/test-classes ray.snake.SnakeUiTest
