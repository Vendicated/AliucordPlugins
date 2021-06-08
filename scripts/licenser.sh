#!/usr/bin/bash
shopt -s globstar

for file in $(git diff --staged --diff-filter=ACMR --name-only | grep -E ".java$")
do
  if [ -f "$file" ] && ! grep -q Copyright "$file"
  then
    cat scripts/licenseHeader.txt "$file" > "$file.licensed" && mv "$file.licensed" "$file"
    git add "$file"
    echo "Added license header to $file"
  else
    echo "Skipped $file as it already had a license header"
  fi
done
