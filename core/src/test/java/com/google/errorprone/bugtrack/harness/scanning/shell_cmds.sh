#!/bin/sh
find . -name '*.java' -type f -exec bash -c 'expand -t 4 $0 | sponge $0' {} \; # normalize tabs
find . -name 'pom.xml' -exec sed -i '/\bmaven.install.skip\b/d' {} \; # no target should be skipped by install
find . -name 'pom.xml' -exec sed -i '/\bmaven.deploy.skip\b/d' {} \; # no target should be skipped by install
find . -name 'pom.xml' -exec sed -i '/\bskipIfEmpty\b/d' {} \; # no target should be skipped by install
find . -name "pom.xml" -exec sed -i '/Xplugin:ErrorProne/d' {} \; # don't run error prone as part of compilation
find . -name "pom.xml" -exec sed -i '/Xlint:all/d' {} \; # don't run error prone as part of compilation
