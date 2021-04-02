#!/bin/bash
for file in "$@"
do
  sed -i "s/_)/__)/g" "$file"
  sed -i "s/_,/__,/g" "$file"
  sed -i "s/_ ->/ __ ->/g" "$file"
done

java -jar "../google-format.jar" -i --skip-sorting-imports --skip-removing-unused-imports "$@"