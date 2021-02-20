#!/bin/python3.8
import re
import sys
import os
import subprocess

# Collect output from mvn install
os.chdir(sys.argv[1])

def extract_raw_args(cmdline):
    return cmdline[cmdline.find("arguments:")+11:]

def print_targets(output):
    build_output = output.split('\n')

    all_compiler_args = [(i, line.strip()) for (i, line) in enumerate(build_output)
                        if "NormalizingJavaCompiler" in line]

    extract_proj_name = re.compile(r"^.*'Compile Java for (.*)'")

    for (i, compiler_arg) in all_compiler_args:
        while extract_proj_name.match(build_output[i]) is None:
            i -= 1

        task_name = extract_proj_name.match(build_output[i]).groups()[0]
        if "compileTestFixturesJava" in task_name:
            continue

        print(task_name)
        print(extract_raw_args(compiler_arg))

gradle_cmd = "./gradlew" if os.path.isfile("./gradlew") else "gradle"

build_proc = subprocess.run([gradle_cmd, "--debug", "--no-build-cache", "classes"],
                            capture_output=True)

test_build_proc = subprocess.run([gradle_cmd, "--debug", "--no-build-cache", "testClasses"],
                                  capture_output=True)

print_targets(build_proc.stdout.decode("utf-8"))
print_targets(test_build_proc.stdout.decode("utf-8"))

# https://docs.oracle.com/javase/9/tools/javac.htm#JSWOR627

# build_output = open("/home/monty/IdeaProjects/java-corpus/okhttp/classes_output").readlines()
