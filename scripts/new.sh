#!/bin/sh
set -e

name="$1"
[ -z "$name" ] && { >&2 echo "Please specify a name"; exit 1; }
first_char="$(printf %.1s "$name")"
[ "$(echo "$first_char"  | tr "[:lower:]" "[:upper:]")" != "$first_char" ] && { >&2 echo "Name must be PascalCase"; exit 1; }

set -x
cp -r Template "$name"
# Rename file
mv "$name/src/main/java/com/aliucord/plugins/Template.java" "$name/src/main/java/com/aliucord/plugins/$name.java"
# Change class name
sed -i "s/Template/$name/" "$name/src/main/java/com/aliucord/plugins/$name.java"
# Add to settings.gradle
echo "include ':$name'" | cat - settings.gradle > settings.gradle.new && mv settings.gradle.new settings.gradle
# Add to updater.json - cat instead of < since < doesn't like piping into same file
# shellcheck disable=SC2002
cat updater.json | jq --argjson "$name" '{"version": "1.0.0","minimumDiscordVersion": 1498}' ". + { $name: \$$name }" | tee updater.json >/dev/null