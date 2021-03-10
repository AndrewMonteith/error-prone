/*
 * Copyright 2021 The Error Prone Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.errorprone.bugtrack.harness.scanning;


import com.google.common.collect.ImmutableSet;
import com.google.errorprone.bugtrack.harness.utils.FileUtils;
import com.google.errorprone.bugtrack.projects.CorpusProject;
import com.google.errorprone.bugtrack.projects.ProjectFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

import static com.google.errorprone.bugtrack.projects.ShouldScanUtils.isJavaFile;

public abstract class ProjectScanner {
    public abstract void cleanProject(Path project) throws IOException, InterruptedException;

    public abstract Collection<DiagnosticsScan> getScans(CorpusProject project)
            throws IOException, InterruptedException;

    protected List<String> filterCmdLineArgs(String rawCmdLineArgs) {
        List<String> args = new ArrayList<>();

        Set<String> singleArgBlockList = ImmutableSet.of("-nowarn", "-deprecation");

        String[] individualArgs = rawCmdLineArgs.split(" ");
        for (int i = 0; i < individualArgs.length; ++i) {
            if (singleArgBlockList.contains(individualArgs[i])) { continue; }
            else if (individualArgs[i].equals("-d")) { ++i; continue; }
            else if (individualArgs[i].startsWith("(") || individualArgs[i].startsWith("[")) { continue; }
            else if (isJavaFile(individualArgs[i])) { continue; }
            else if (individualArgs[i].equals("-target") || individualArgs[i].equals("-source")) {
                individualArgs[i + 1] = "1.8";
                ++i;
                continue;
            }
            else if (individualArgs[i].startsWith("-Xlint")) { continue; }
            else if (individualArgs[i].startsWith("-Xdoclint")) { continue; }
            else if (individualArgs[i].startsWith("-W")) { continue; }

            args.add(individualArgs[i]);
        }

        return args;
    }

    protected List<ProjectFile> getFilesFromSourcepath(CorpusProject project, String sourcepath) {
        if (sourcepath.contains(":")) {
            throw new RuntimeException("this method can only scan a single sourcepath");
        }

        return FileUtils.findFilesMatchingGlob(Paths.get(sourcepath), "**/*.java").stream()
                .filter(project::shouldScanFile)
                .map(file -> new ProjectFile(project, file))
                .collect(Collectors.toList());
    }
}
