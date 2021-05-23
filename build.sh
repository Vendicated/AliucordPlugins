#/bin/sh
# POSIX Shell script to build plugin, push it to device, and reopen discord via adb.
set -e
old_dir=$PWD
cd ../buildtool
./main -p $1
cd ./buildsPlugins
adb push ./${1}.apk /storage/emulated/0/Aliucord/plugins
adb shell am force-stop com.aliucord
adb shell monkey -p com.aliucord -c android.intent.category.LAUNCHER 1
cd $old_dir