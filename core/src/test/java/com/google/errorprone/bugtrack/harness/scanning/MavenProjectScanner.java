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

import com.google.errorprone.bugtrack.harness.utils.ShellUtils;
import com.google.errorprone.bugtrack.projects.CorpusProject;
import com.google.errorprone.bugtrack.projects.ProjectFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public class MavenProjectScanner extends ProjectScanner {
    @Override
    public void cleanProject(Path projectDir) throws IOException, InterruptedException {
        ShellUtils.runCommand(projectDir, "mvn", "clean");
    }

    private List<ProjectFile> getFilesFromSourcepaths(CorpusProject project, String sourcepaths, Set<String> scannedSourcepaths) {
        return Arrays.stream(sourcepaths.split(":"))
                .filter(sourcepath -> !scannedSourcepaths.contains(sourcepath))
                .peek(scannedSourcepaths::add)
                .map(sourcepath -> getFilesFromSourcepath(project, sourcepath))
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }

    public Collection<DiagnosticsScan> parseScansOutput(CorpusProject project, String buildOutput) {
        Collection<DiagnosticsScan> scans = new ArrayList<>();
        Set<String> scannedSourcepaths = new HashSet<>();

        String[] buildOutputLines = buildOutput.split("\n");
        for (int i = 0; i < buildOutputLines.length; i += 2) {
            String scanName = buildOutputLines[i];
            List<String> cmdLineArguments = filterCmdLineArgs(buildOutputLines[i + 1]);

            List<ProjectFile> filesToParse = getFilesFromSourcepaths(project,
                    cmdLineArguments.get(cmdLineArguments.indexOf("-sourcepath") + 1), scannedSourcepaths);

            scans.add(new DiagnosticsScan(
                    scanName,
                    filesToParse,
                    cmdLineArguments));
        }

        return scans;
    }

    @Override
    public Collection<DiagnosticsScan> getScans(CorpusProject project) throws IOException, InterruptedException {
        String buildOutput = ShellUtils.runCommand(project.getRoot(),
                "/usr/bin/python3.8",
                "/home/monty/IdeaProjects/error-prone/core/src/test/java/com/google/errorprone/bugtrack/harness/scanning/get_maven_cmdargs.py",
                project.getRoot().toString());

        return parseScansOutput(project, buildOutput);
    }
}
