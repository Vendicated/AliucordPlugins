#!/bin/sh
set -e

(
  die() {
    echo "$@"
    exit 1
  } >&2

  [ "$1" != "*" ] && [ ! -d "$1" ] && die "Usage: $0 [PLUGIN_FOLDER]"
  command -v d8 >/dev/null || export PATH="/home/ven/Android/Sdk/build-tools/30.0.3:$PATH"

  cd ../buildtool
  echo "Building plugin..."
  ./buildtool -p "$1"

  cd ../buildsPlugins
  [ "$(adb devices | wc -l)" = "2" ] && adb connect 192.168.178.53:5555

  echo "Pushing plugin zip to device..."
  if [ "$1" = "*" ]; then
    adb push -- *.zip /storage/emulated/0/Aliucord/plugins
  else
    adb push "$1.zip" /storage/emulated/0/Aliucord/plugins
  fi

  echo "Force stopping Aliucord..."
  adb shell am force-stop com.aliucord

  echo "Launching Aliucord..."
  adb shell monkey -p com.aliucord -c android.intent.category.LAUNCHER 1
)
