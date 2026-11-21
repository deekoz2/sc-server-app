# Copyright 2026 deekoz2 — Apache License 2.0
# 生存战争服务端控制器：由配套 Android APK 经 Termux RUN_COMMAND 调用
#!/data/data/com.termux/files/usr/bin/sh
# 生存战争服务端控制器 —— 由「生存战争服务端」APK 通过 Termux RUN_COMMAND 调用
# 用法: sc-ctl.sh start|stop|status
DIR=/sdcard/server
ST=$DIR/.sc-status
LOG=$DIR/run.log
PIDF=$DIR/.sc.pid

# 兼容 ZeroTermux 的存储路径软链
[ -d "$DIR" ] || DIR=/storage/emulated/0/server
[ -d "$DIR" ] || { echo "ERROR no-dir" > "$ST" 2>/dev/null || { mkdir -p /sdcard/server; echo "ERROR no-dir" > /sdcard/server/.sc-status; }; exit 1; }

case "$1" in
  start)
    if [ -f "$PIDF" ] && kill -0 "$(cat "$PIDF")" 2>/dev/null; then
      echo "RUNNING $(cat "$PIDF")" > "$ST"; exit 0
    fi
    echo "STARTING" > "$ST"
    echo "[$(date '+%m-%d %H:%M:%S')] 启动中..." >> "$LOG"
    cd "$DIR" || exit 1
    if command -v dotnet >/dev/null 2>&1; then
      nohup dotnet Survivalcraft.dll >> "$LOG" 2>&1 &
    elif command -v proot-distro >/dev/null 2>&1; then
      nohup proot-distro login ubuntu -- sh -c "cd /sdcard/server && exec dotnet Survivalcraft.dll" >> "$LOG" 2>&1 &
    else
      echo "ERROR no-dotnet" > "$ST"
      echo "[$(date '+%m-%d %H:%M:%S')] 未找到 dotnet：请在 ZeroTermux 里安装（详见 APK 里的安装指引）" >> "$LOG"
      exit 1
    fi
    echo $! > "$PIDF"
    echo "RUNNING $!" > "$ST"
    ;;
  stop)
    if [ -f "$PIDF" ]; then kill "$(cat "$PIDF")" 2>/dev/null; fi
    pkill -f "dotnet.*Survivalcraft" 2>/dev/null
    pkill -f "proot-distro login ubuntu" 2>/dev/null
    rm -f "$PIDF"
    echo "STOPPED" > "$ST"
    echo "[$(date '+%m-%d %H:%M:%S')] 已停止" >> "$LOG"
    ;;
  status)
    if [ -f "$PIDF" ] && kill -0 "$(cat "$PIDF")" 2>/dev/null; then
      echo "RUNNING $(cat "$PIDF")" > "$ST"
    else
      rm -f "$PIDF"
      echo "STOPPED" > "$ST"
    fi
    ;;
esac

tail -n 24 "$LOG" > "$DIR/.sc-log-tail" 2>/dev/null || true
