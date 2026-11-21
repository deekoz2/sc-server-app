#!/bin/bash
# 生存战争服务端控制 App —— 纯命令行构建脚本（无 Gradle）
# 已在 arm64 Linux (Ubuntu 24.04 proot) 验证
# 依赖：apt install aapt apksigner zipalign dalvik-exchange openjdk-17-jdk-headless
# 用法：bash build.sh /path/to/android.jar
set -e

ANDROID_JAR="${1:?用法: bash build.sh /path/to/android.jar}"
cd "$(dirname "$0")"
BUILD=build
mkdir -p "$BUILD/classes" dist

echo "[1/5] javac 编译"
javac -source 8 -target 8 -bootclasspath "$ANDROID_JAR" \
      -d "$BUILD/classes" app/src/com/dsh/scserver/MainActivity.java

echo "[2/5] dx 转 dex"
DX="$(command -v dx || echo /usr/lib/android-sdk/build-tools/debian/dx)"
"$DX" --dex --output="$BUILD/classes.dex" "$BUILD/classes"

echo "[3/5] aapt 打包资源与清单"
aapt package -f -M app/AndroidManifest.xml -I "$ANDROID_JAR" -F "$BUILD/unsigned.apk"
aapt add "$BUILD/unsigned.apk" "$BUILD/classes.dex"

echo "[4/5] zipalign"
zipalign -f 4 "$BUILD/unsigned.apk" "$BUILD/aligned.apk"

echo "[5/5] 签名"
KS="$BUILD/debug.keystore"
if [ ! -f "$KS" ]; then
  keytool -genkeypair -keystore "$KS" -alias dsh \
    -storepass dsh12345 -keypass dsh12345 \
    -dname "CN=DSH" -keyalg RSA -validity 10000 2>/dev/null
fi
apksigner sign --ks "$KS" --ks-pass pass:dsh12345 \
  --out "dist/生存战争服务端.apk" "$BUILD/aligned.apk"
apksigner verify "dist/生存战争服务端.apk"
echo "完成：dist/生存战争服务端.apk"
