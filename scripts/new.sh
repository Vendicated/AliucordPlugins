#!/bin/sh
set -e

name="$1"
[ -z "$name" ] && { >&2 echo "Please specify a name"; exit 1; }
first_char="$(printf %.1s "$name")"
[ "$(echo "$first_char"  | tr "[:lower:]" "[:upper:]")" != "$first_char" ] && { >&2 echo "Name must be PascalCase"; exit 1; }

set -x
cp -r Template "$name"
# Rename file
mv "$name/src/main/java/com/aliucord/plugins/Template.kt" "$name/src/main/java/com/aliucord/plugins/$name.kt"
# Change class name
sed -i "s/Template/$name/" "$name/src/main/java/com/aliucord/plugins/$name.kt"
# Add to settings.gradle
echo "include(\":$name\")" | cat - settings.gradle.kts > settings.gradle.new && mv settings.gradle.new settings.gradle.kts
# Add to updater.json
jq --argjson "$name" '{"version": "1.0.0"}' ". + { $name: \$$name }" < updater.json \
  > updater.json.new && \
  mv updater.json.new updater.json