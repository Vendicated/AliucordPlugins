#!/usr/bin/bash
shopt -s globstar

for file in **/*.java
do
  if [ -f "$file" ] && ! grep -q Copyright "$file"
  then
    cat scripts/licenseHeader.txt "$file" > "$file.licensed" && mv "$file.licensed" "$file"
    git add "$file"
  fi
done
