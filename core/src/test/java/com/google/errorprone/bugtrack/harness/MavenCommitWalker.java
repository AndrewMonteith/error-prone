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

package com.google.errorprone.bugtrack.harness;

import com.google.errorprone.bugtrack.projects.CorpusProject;
import com.google.errorprone.bugtrack.projects.ProjectFile;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MavenCommitWalker extends ProjectScanner {
    private Map<String, DiagnosticsScan> scans;

    public MavenCommitWalker() {
        scans = new HashMap<>();
    }

    @Override
    public void cleanProject(File projectDir) throws IOException, InterruptedException {
        ShellUtils.runCommand(projectDir, "mvn", "clean");
    }

    @Override
    public Collection<DiagnosticsScan> getScans(CorpusProject project) throws IOException, InterruptedException {
        String scriptOutput = ShellUtils.runCommand(new File(project.getRoot()),
                "/usr/bin/python3.8",
                "/home/monty/IdeaProjects/error-prone/core/src/test/java/com/google/errorprone/bugtrack/harness/get_maven_cmdargs.py",
                project.getRoot());

        /*
            As we wind the commits forward we need to be watch for a few things.
              1. Files added by a commit should be included alongside unchanged/changed files.
              2. Files detected should not be scanned again.

            In the case of 1, maven will generate a command to recompile the file. That command
            will include the sourcepath which we scan to check for files added/removed.

            In the case of 2, there is a guard on the diagnostics collector to ensure only
            files on disk are scanned.
         */

        String[] scriptOutputLines = scriptOutput.split("\n");
        for (int i = 0; i < scriptOutputLines.length; i += 2) {
            String scanName = scriptOutputLines[i];
            List<String> cmdLineArguments = filterCmdLineArgs(scriptOutputLines[i + 1]);
            List<ProjectFile> filesToParse = getFilesFromSourcepath(project,
                    cmdLineArguments.get(cmdLineArguments.indexOf("-sourcepath") + 1));

            scans.put(scanName, new DiagnosticsScan(
                    scanName,
                    filesToParse,
                    cmdLineArguments));
        }

        return scans.values();
    }
}
