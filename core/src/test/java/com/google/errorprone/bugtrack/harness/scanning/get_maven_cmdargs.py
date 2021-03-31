#!/bin/python3
import re
import random
import sys
import os
import subprocess

# Collect output from mvn install
os.chdir(sys.argv[1])

stdout_file = "stdout_" + str(random.randint(1, 10000))

# subprocess.PIPE can hang for sufficient large outputs, so we pipe everything into text files
with open(stdout_file, "w") as f:
    # Parse mvn install for command line options
    install_proc = subprocess.run(["mvn", "-X", "-e", "-T", "1C", "clean", "install", "-Dmaven.javadoc.skip=true", "-DskipTests",
                                   "-Dcheckstyle.skip=true", "-Drat.skip=true", "-Dcheckbugs.skip=true", "-Dspotbugs.skip=true",
                                   "-Denforcer.skip=true", "-Dmaven.checkstyle.skip=true", "-Dguice.with.jarjar=false"],
                                  stdout=f)

def escape_ansi(line):
    ansi_escape = re.compile(r"(?:\x1B[@-_]|[\x80-\x9F])[0-?]*[ -/]*[@-~]")
    return ansi_escape.sub("", line)

build_output = [escape_ansi(line)
                for line in open(stdout_file, "r").readlines()]

# build_output = [escape_ansi(line)
#                 for line in open("build_output", "r").readlines()]

proj_re = re.compile(r"^.*\[.*INFO.*\].* @ .*")
proj_name_re = re.compile(r"^\[INFO\] (?:\-\-\-|\>\>\>) .* \((.*)\).*@.*(.*) .*$")

cmdline_options = [i for (i, line) in enumerate(build_output)
                     if "Command line options:" in line]

for i in cmdline_options:
    if "-d" not in build_output[i+1]:
        continue

    cmdline_args = build_output[i+1][7:].strip()
    proj_line = next(build_output[i] for i in range(i, 0, -1)
                     if proj_re.match(build_output[i])).strip()

    (target, name) = proj_name_re.match(proj_line).groups()
    print(target, name)
    print(cmdline_args)
    
