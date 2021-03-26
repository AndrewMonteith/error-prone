#!/bin/bash
find . -name 'pom.xml' -exec sed -i '/\bmaven.install.skip\b/d' {} \; # no target should be skipped by install
find . -name 'pom.xml' -exec sed -i '/\bmaven.deploy.skip\b/d' {} \; # no target should be skipped by install
find . -name 'pom.xml' -exec sed -i '/\bskipIfEmpty\b/d' {} \; # no target should be skipped by install
find . -name "pom.xml" -exec sed -i '/Xplugin:ErrorProne/d' {} \; # don't run error prone as part of compilation
find . -name "pom.xml" -exec sed -i '/Xlint:all/d' {} \; # don't run error prone as part of compilation
find . -name "pom.xml" -exec sed -i 's/ xmlns.*=".*"//g' {} \; # strip namespaces to make xpath manageable
find . -name "pom.xml" -exec bash -c 'xml ed -d "//plugin[artifactId=\"findbugs-maven-plugin\"]" "$0" | sponge "$0"' {} \; # early versions can't be disabled via cmdline
find . -name "pom.xml" -exec bash -c 'xml ed -d "//plugin[artifactId=\"spotbugs-maven-plugin\"]" "$0" | sponge "$0"' {} \; # early versions can't be disabled via cmdline
find . -name "ant-phase-verify.xml" -exec sed -i 's/failOnViolation="true"/failOnViolation="false"/g' {} \;