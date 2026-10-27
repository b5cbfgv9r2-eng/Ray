Ray Snake · Java 贪吃蛇

这是电脑上的桌面小游戏，不能在手机或浏览器里直接运行。

1. 先把 ZIP 解压到一个文件夹，不要在压缩包里直接打开游戏。
2. 电脑需要 Java 17 或更新版本。没有安装的话，可前往：
   https://adoptium.net/temurin/releases/
3. Windows：双击 play.bat。
   macOS / Linux：在解压后的目录打开终端，执行 bash play.sh。
   也可以在这个目录执行 java -jar ray-snake.jar。

Enter 开始，方向键或 WASD 移动，空格暂停 / 继续，R 重新开始。
开始前可选择 Easy（简单）、Normal（普通）、Fast（快速）。
吃到红色食物加 10 分；撞墙或撞到自己就结束。
最高分按难度分别保存在用户主目录的 .ray-snake 文件夹。

源码与中文说明：https://github.com/b5cbfgv9r2-eng/Ray
