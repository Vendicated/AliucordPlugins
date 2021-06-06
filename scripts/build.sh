#!/bin/sh
# shellcheck disable=SC2064
set -e
trap "cd '$PWD'" EXIT

# add build tools to path for d8
export PATH=/home/ven/Android/Sdk/build-tools/30.0.3:$PATH

set -x

cd ../buildtool
./buildtool -p "$1"

cd ../buildsPlugins
adb push "${1}.zip" /storage/emulated/0/Aliucord/plugins
adb shell am force-stop com.aliucord
adb shell monkey -p com.aliucord -c android.intent.category.LAUNCHER 1