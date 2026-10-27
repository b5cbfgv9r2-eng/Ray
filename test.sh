#!/usr/bin/env bash
set -euo pipefail
cd -- "$(dirname -- "$0")"
bash ./build.sh
mkdir -p build/test-classes
javac --release 17 -encoding UTF-8 -Xlint:all -cp build/classes -d build/test-classes src/test/java/ray/snake/*.java
java -Djava.awt.headless=true -cp build/classes:build/test-classes ray.snake.SnakeGameTest
java -Djava.awt.headless=true -cp build/classes:build/test-classes ray.snake.HighScoreStoreTest
