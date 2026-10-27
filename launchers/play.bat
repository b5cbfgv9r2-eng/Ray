@echo off
setlocal
cd /d "%~dp0"
where java >nul 2>&1
if errorlevel 1 (
  echo Please install Java 17 or newer from https://adoptium.net/temurin/releases/
  pause
  exit /b 1
)
java -jar ray-snake.jar
if errorlevel 1 (
  echo The game needs Java 17 or newer and a desktop display.
  pause
  exit /b 1
)
