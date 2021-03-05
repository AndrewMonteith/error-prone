#!/bin/python3
import re
import sys
import os
import subprocess

# Collect output from mvn install
os.chdir(sys.argv[1])

# Parse mvn install for command line options
install_proc = subprocess.run(["mvn", "-X", "install", "-Dmaven.javadoc.skip=true", "-DskipTests"],
                              stdout=subprocess.PIPE, stderr=subprocess.PIPE)

def escape_ansi(line):
    ansi_escape = re.compile(r"(?:\x1B[@-_]|[\x80-\x9F])[0-?]*[ -/]*[@-~]")
    return ansi_escape.sub("", line)

build_output = [escape_ansi(line)
                for line in install_proc.stdout.decode('utf-8').split("\n")]
# build_output = [escape_ansi(line)
#                 for line in open("/home/monty/IdeaProjects/java-corpus/jsoup/jsoup_output").readlines()]

proj_re = re.compile(r"^.*\[.*INFO.*\].* @ .*")
proj_name_re = re.compile(r"^\[INFO\] --- .* \((.*)\) @ (.*) .*")

cmdline_options = [i for (i, line) in enumerate(build_output)
                     if "Command line options:" in line]

for i in cmdline_options:
    cmdline_args = build_output[i+1][7:].strip()
    proj_line = next(build_output[i] for i in range(i, 0, -1)
                     if proj_re.match(build_output[i])).strip()

    (target, name) = proj_name_re.match(proj_line).groups()
    print(target, name)
    print(cmdline_args)
    
