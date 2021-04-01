#!/bin/bash
# Don't skip files
find . -name 'pom.xml' -exec sed -i '/\bmaven.install.skip\b/d' {} \; # no target should be skipped by install
find . -name 'pom.xml' -exec sed -i '/\bmaven.deploy.skip\b/d' {} \; # no target should be skipped by install
find . -name 'pom.xml' -exec sed -i '/\bskipIfEmpty\b/d' {} \; # no target should be skipped by install

# Don't already have error prone installed
find . -name "pom.xml" -exec sed -i '/Xplugin:ErrorProne/d' {} \; # don't run error prone as part of compilation
find . -name "pom.xml" -exec sed -i '/Xlint:all/d' {} \; # don't run error prone as part of compilation
find . -name "pom.xml" -exec sed -i 's/ xmlns.*=".*"//g' {} \; # strip namespaces to make xpath manageable

# Skip some slow plugins
find . -name "pom.xml" -exec bash -c 'xml ed -d "//plugin[artifactId=\"findbugs-maven-plugin\"]" "$0" | sponge "$0"' {} \; # early versions can't be disabled via cmdline
find . -name "pom.xml" -exec bash -c 'xml ed -d "//plugin[artifactId=\"spotbugs-maven-plugin\"]" "$0" | sponge "$0"' {} \; # early versions can't be disabled via cmdline

# Ensure all symbols are preserved in class files
find . -name "pom.xml" -exec bash -c 'xml ed -s "//plugin[artifactId=\"maven-compiler-plugin\" and not(configuration)]" -t elem -n "configuration" "$0" | sponge "$0"' {} \; # insert compilerArgs node
find . -name "pom.xml" -exec bash -c 'xml ed -s "//plugin[artifactId=\"maven-compiler-plugin\"]/configuration[not(compilerArgs)]" -t elem -n "compilerArgs" "$0" | sponge "$0"' {} \; # insert compilerArgs node
find . -name "pom.xml" -exec bash -c 'xml ed -s "//plugin[artifactId=\"maven-compiler-plugin\"]/configuration/compilerArgs" -t elem -n "arg" -v "-parameters" "$0" | sponge "$0"' {} \;  # keep parameters
find . -name "pom.xml" -exec bash -c 'xml ed -s "//plugin[artifactId=\"maven-compiler-plugin\"]/configuration[not(compilerArguments)]" -t elem -n "compilerArguments" "$0" | sponge "$0"' {} \; # insert compilerArgs node
find . -name "pom.xml" -exec bash -c 'xml ed -s "//plugin[artifactId=\"maven-compiler-plugin\"]/configuration/compilerArguments" -t elem -n "parameters" "$0" | sponge "$0"' {} \; # insert compilerArgs node

# Update jdk 1.5, 1.6, 1.7 -> 1.8
#find . -name "pom.xml" -exec bash -c 'xml ed -u "//plugin[artifactId=\"maven-compiler-plugin\"]//source[text()=\"${jdk.version}\" or text()=\"1.5\" or text()=\"1.6\" or text()=\"1.7\"]" -v "1.8" "$0"  | sponge "$0"' {} \;  # keep parameters
#find . -name "pom.xml" -exec bash -c 'xml ed -u "//plugin[artifactId=\"maven-compiler-plugin\"]//target[text()=\"${jdk.version}\"or text()=\"1.5\" or text()=\"1.6\" or text()=\"1.7\"]" -v "1.8" "$0"  | sponge "$0"' {} \;  # keep parameters
#find . -name "pom.xml" -exec bash -c 'xml ed -u "//jdk.version///target[text()=\"${jdk.version}\"or text()=\"1.5\" or text()=\"1.6\" or text()=\"1.7\"]" -v "1.8" "$0"  | sponge "$0"' {} \;  # keep parameters
#find . -name "pom.xml" -exec sed -i 's/<jdk.version>1.5/<jdk.version>1.8/g' {} \;
#find . -name "pom.xml" -exec sed -i 's/<jdk.version>1.6/<jdk.version>1.8/g' {} \;
#find . -name "pom.xml" -exec sed -i 's/<jdk.version>1.7/<jdk.version>1.8/g' {} \;
#find . -name "pom.xml" -exec sed -i 's/<source>1.5/<source>1.8/g' {} \;
#find . -name "pom.xml" -exec sed -i 's/<source>${jdk.version}/<source>1.8/g' {} \;
#find . -name "pom.xml" -exec sed -i 's/<source>1.6/<source>1.8/g' {} \;
#find . -name "pom.xml" -exec sed -i 's/<source>1.7/<source>1.8/g' {} \;
find . -name "pom.xml" -exec sed -i 's/<target>${jdk.version}/<target>1.8/g' {} \;
find . -name "pom.xml" -exec sed -i 's/<target>1.5/<target>1.8/g' {} \;
find . -name "pom.xml" -exec sed -i 's/<target>1.6/<target>1.8/g' {} \;
find . -name "pom.xml" -exec sed -i 's/<target>1.7/<target>1.8/g' {} \;

# Misc
find . -name "ant-phase-verify.xml" -exec sed -i 's/failOnViolation="true"/failOnViolation="false"/g' {} \;
