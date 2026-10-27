#!/usr/bin/env bash
set -euo pipefail
cd -- "$(dirname -- "$0")"
if ! command -v java >/dev/null; then
  echo 'Please install Java 17 or newer: https://adoptium.net/temurin/releases/' >&2
  exit 1
fi
exec java -jar ray-snake.jar
