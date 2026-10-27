@echo off
setlocal
cd /d "%~dp0"
where javac >nul 2>&1
if errorlevel 1 (
  echo Please install JDK 17 or newer and add its bin directory to PATH.
  pause
  exit /b 1
)
if not exist build\classes mkdir build\classes
if not exist dist mkdir dist
javac --release 17 -encoding UTF-8 -d build\classes src\main\java\ray\snake\*.java
if errorlevel 1 goto failed
jar --create --file dist\ray-snake.jar --main-class ray.snake.SnakeApp -C build\classes .
if errorlevel 1 goto failed
java -jar dist\ray-snake.jar
if errorlevel 1 goto failed
exit /b 0
:failed
echo The game could not start. Check the error message above.
pause
exit /b 1
