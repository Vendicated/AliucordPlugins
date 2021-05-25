#!/bin/sh
# shellcheck disable=SC2064
trap "cd '$PWD'" EXIT
set -ex

cd ../buildtool
./buildtool -p "$1"

cd ../buildsPlugins
adb push "${1}.zip" /storage/emulated/0/Aliucord/plugins
adb shell am force-stop com.aliucord
adb shell monkey -p com.aliucord -c android.intent.category.LAUNCHER 1
