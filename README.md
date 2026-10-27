# Ray Snake · Java 贪吃蛇

一个用 **Java + Swing** 制作的桌面小游戏。无需 Maven、Gradle 或第三方依赖，支持 Windows、macOS、Linux。

## 直接下载游戏

**[点击下载贪吃蛇 ZIP](https://github.com/b5cbfgv9r2-eng/Ray/raw/refs/heads/main/downloads/ray-snake.zip)**

先解压，Windows 双击里面的 `play.bat`；macOS / Linux 在解压目录执行 `bash play.sh`。电脑需要安装 Java 17 或更新版本。详细步骤见压缩包内的 `README.txt`。这是桌面游戏，手机和浏览器不能直接运行。

## 怎么开始玩

需要 **JDK 17 或更新版本**（例如 Eclipse Temurin 21）。安装时将 Java 加入 PATH。

- **Windows**：双击仓库里的 `run.bat`。
- **macOS / Linux**：在项目目录打开终端，执行 `./run.sh`。

启动脚本会先编译，再弹出游戏窗口。界面使用英文，操作如下：

| 操作 | 按键 / 按钮 |
| --- | --- |
| 开始 / 再来一局 | Enter / Start / Play again |
| 移动 | 方向键或 W A S D |
| 暂停 / 继续 | 空格 / Pause / Resume |
| 立即重新开始 | R / Restart |
| 选择难度 | 开始前选择 Easy（简单）、Normal（普通）、Fast（快速） |

吃到红色食物加 **10 分**，蛇会变长；撞墙或撞到自己就结束，填满棋盘则获胜。不能直接掉头，每次移动之间只接受一次转向。切换到其他窗口会自动暂停，回来后按空格继续。

最高分按难度分别保存到用户主目录的 `.ray-snake/high-scores.properties`，关闭游戏后仍会保留。无法写入时仍可正常玩，本次最高分保留到退出。

## 已编译的游戏

执行 `./build.sh` 后会生成 `dist/ray-snake.jar`。复制这个 JAR 到装有 **Java 17+** 和桌面环境的电脑，然后运行：

```sh
java -jar dist/ray-snake.jar
```

JAR 运行只需要 Java 运行时；修改代码和重新编译需要完整 JDK。`dist/` 和 `build/` 是生成目录，不提交到 Git。

## 开发与测试

```sh
./build.sh       # 编译并打包
./test.sh        # 游戏规则、最高分存储测试，不需要显示器
./gui-test.sh    # 实际窗口、键盘、暂停、重开测试，需要桌面 / Xvfb
./package.sh     # 生成包含 JAR、启动脚本和说明的下载 ZIP
```

源码位于 `src/main/java/ray/snake/`：`SnakeGame` 管理规则，`SnakeApp` 绘制窗口并处理输入，`HighScoreStore` 保存纪录。测试位于 `src/test/java/ray/snake/`，使用独立断言，无需安装测试框架。

可通过 `java -Dray.snake.dataDir=/your/directory -jar dist/ray-snake.jar` 指定最高分保存目录。

## 云端环境

云端已安装的 JDK 可这样启用：

```sh
export JAVA_HOME=/workspace/shared/java/jdk-21.0.13+9
export PATH="$JAVA_HOME/bin:$PATH"
cd /workspace/Ray
./test.sh
```

这是桌面程序，云端聊天界面没有交互式桌面，也没有网页预览。实际游玩请在自己的电脑上运行 JAR。云端可以编译、修改代码、运行规则测试，并通过已安装的虚拟显示器检查窗口：

```sh
/workspace/shared/xvfb/usr/bin/Xvfb :99 -screen 0 1280x1024x24 -nolisten tcp &
ray_display_pid=$!
trap 'kill "$ray_display_pid"' EXIT
DISPLAY=:99 ./gui-test.sh
```

窗口测试会生成 `build/snake-preview.png`。虚拟显示器和游戏进程需要重新启动，不能假定跨任务保留。每个云任务已隔离，使用现有 `/workspace/Ray`，无需额外创建 Git worktree。
