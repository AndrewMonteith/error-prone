#!/bin/python3
import os.path
import re
import subprocess
import sys
from pathlib import Path

# Collect output from mvn install
os.chdir(sys.argv[1])


def get_current_commit():
    git_query = subprocess.run(["git", "log", "-1", "--oneline"], stdout=subprocess.PIPE)
    output = git_query.stdout.decode("utf-8").split(" ")
    return output[0]

ROOT = Path(os.getenv("ROOT") if os.getenv("ROOT") is not None else "/home/monty/IdeaProjects")

build_output_folder = ROOT / "build_outputs" / Path(sys.argv[1]).name
if not os.path.isdir(build_output_folder):
    os.mkdir(build_output_folder)

stdout_file = build_output_folder / ("stdout_" + get_current_commit())

# subprocess.PIPE can hang for sufficient large outputs, so we pipe everything into text files
with open(stdout_file, "w") as f:
    # Parse mvn install for command line options
    install_proc = subprocess.run(
        ["mvn", "-X", "-e", "-T", "1C", "clean", "install", "-Dmaven.javadoc.skip=true", "-DskipTests",
         "-Dcheckstyle.skip=true", "-Drat.skip=true", "-Dcheckbugs.skip=true", "-Dspotbugs.skip=true",
         "-Denforcer.skip=true", "-Dmaven.checkstyle.skip=true", "-Dguice.with.jarjar=false", "-Dtidy.skip=true"],
        stdout=f)


def escape_ansi(line):
    ansi_escape = re.compile(r"(?:\x1B[@-_]|[\x80-\x9F])[0-?]*[ -/]*[@-~]")
    return ansi_escape.sub("", line)


build_output = [escape_ansi(line)
                for line in open(stdout_file, "r").readlines()]

# build_output = [escape_ansi(line)
#                 for line in open("/home/monty/IdeaProjects/foo", "r").readlines()]

i = 0

for line in build_output:
    # We assume every command line blob has this, and look forward to being proved wrong otherwise.
    if not (" -d " in line and "-parameters" in line):
        continue

    print("blob", i)  # (target, name)
    print(line[7:].strip())  # command line args
    i += 1

# When using parallel builds there's a change lines could be put between "Command line options"
# So technically scraping the output is a race condition.
# proj_re = re.compile(r"^.*\[.*INFO.*\].* @ .*")
# proj_name_re = re.compile(r"^\[INFO\] (?:\-\-\-|\>\>\>) .* \((.*)\).*@.*(.*) .*$")
#
# cmdline_options = [i for (i, line) in enumerate(build_output)
#                      if "Command line options:" in line]
#
# for i in cmdline_options:
#     if "-d" not in build_output[i+1]:
#         # Try and find the '-d' line a few below
#         continue
#
#     cmdline_args = build_output[i+1][7:].strip()
#     proj_line = next(build_output[i] for i in range(i, 0, -1)
#                      if proj_re.match(build_output[i])).strip()
#
#     (target, name) = proj_name_re.match(proj_line).groups()
#     print(target, name)
#     print(cmdline_args)
#
