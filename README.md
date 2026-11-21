# 生存战争服务端控制 App

一个简洁的 Android APK，用来在手机上**一键启动 / 停止 [SurvivalcraftNet](https://gitee.com/SC-SPM/SurvivalcraftNet) 生存战争服务端**，实时查看运行状态和日志。

App 本身不包含服务端和游戏本体，它是一个「遥控器」：通过 [Termux](https://github.com/termux/termux-app) 的 `RUN_COMMAND` 机制，在 ZeroTermux 环境里执行控制脚本，用 dotnet 10 拉起 `Survivalcraft.dll`。

## 功能

- ▶ 一键启动 / ■ 一键停止服务端
- 自动刷新的运行状态（运行中 / 已停止 / 启动中 / 配置错误）
- 最近 24 行运行日志
- Termux 包名可配置（默认 `com.termux`，兼容 ZeroTermux 其他包名）

## 安装使用

1. 下载 [Releases](../../releases) 里的 APK 安装（需允许未知来源）
2. 安装并打开 [ZeroTermux](https://github.com/termux/termux-app/releases)（或原版 Termux），做一次性配置（App 内「Termux 配置说明」可复制全部命令）：

```sh
# 允许外部 App 调用（必须）
echo 'allow-external-apps=true' >> ~/.termux/termux.properties
# 然后完全退出并重开 ZeroTermux

# 存储权限
termux-setup-storage

# 安装 .NET 10（Termux 官方源）
pkg update && pkg install dotnet-sdk-10.0
```

3. 服务端放在 `/sdcard/server/`（含 `Survivalcraft.dll` 与 `sc-ctl.sh`）
4. 打开 App → 点 ▶ 启动 → 日志出现服务端启动信息即成功

> 系统会要求给 App「所有文件访问」权限，用于读取状态和日志文件。

## 从源码构建

在 arm64 Linux 环境（如手机 proot Ubuntu 24.04）验证过的纯命令行构建，不需要 Gradle：

```sh
apt install aapt apksigner zipalign dalvik-exchange openjdk-17-jdk-headless
# android.jar 来自 platform-34：https://dl.google.com/android/repository/platform-34-ext7_r03.zip
bash app/build.sh /path/to/android.jar
# 产物：dist/生存战争服务端.apk
```

## 目录结构

```
app/src/com/dsh/scserver/MainActivity.java   # 界面与 RUN_COMMAND 调用
app/AndroidManifest.xml                      # 权限与入口
app/build.sh                                 # 纯命令行构建脚本（无 Gradle）
sc-ctl.sh                                    # 服务端控制脚本（由 APK 调用执行）
```

## 工作原理

```
[APK 按钮] --RUN_COMMAND intent--> [ZeroTermux] --执行--> sc-ctl.sh start
     ↑                                                    │
     └──── 读取 /sdcard/server/.sc-status / .sc-log-tail ←──┘
                                            （后台 dotnet Survivalcraft.dll）
```

## 许可证

- 本项目以 [Apache-2.0](LICENSE) 开源。
- 致谢：[Termux](https://github.com/termux/termux-app)（RUN_COMMAND 机制）、[SurvivalcraftNet](https://gitee.com/SC-SPM/SurvivalcraftNet)（生存战争服务端）、[ValleyServer](https://github.com/Lixeer/ValleyServer)（设计思路参考）。
- 本 App 不分发《生存战争》游戏本体及服务端程序；游戏版权归 Candy Rufus Games 所有。
