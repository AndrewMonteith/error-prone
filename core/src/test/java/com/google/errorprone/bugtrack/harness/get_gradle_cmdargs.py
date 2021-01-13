#!/bin/python3.8
import re
import sys
import os
import subprocess

# Collect output from mvn install
# os.chdir(sys.argv[1])

# Parse mvn install for command line options
# build_proc = subprocess.run(["gradle", "-d", "classes"],
#                             capture_output=True)

# https://docs.oracle.com/javase/9/tools/javac.htm#JSWOR627

build_output = open("/home/monty/IdeaProjects/java-corpus/spring-framework/output").readlines()

all_compiler_args = [(i, line) for (i, line) in enumerate(build_output)
                     if "Compiler arguments:" in line]

extract_proj_name = re.compile(r"^.*'Compile Java for (.*)'")

for (i, compiler_arg) in all_compiler_args:
    task_name = extract_proj_name.match(build_output[i-1]).groups()[0]
    print(task_name)
    print(extract_proj_name)