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

import com.google.common.base.Joiner;
import com.google.common.collect.Iterables;
import com.google.errorprone.bugtrack.harness.utils.FileUtils;
import com.google.errorprone.bugtrack.harness.utils.ShellUtils;
import com.google.errorprone.bugtrack.projects.CorpusProject;
import com.google.errorprone.bugtrack.projects.ProjectFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public class GradleProjectScanner extends ProjectScanner {
    private final Map<String, DiagnosticsScan> scans;

    public GradleProjectScanner() {
        this.scans = new HashMap<>();
    }

    @Override
    public void cleanProject(File project) throws IOException, InterruptedException {
        ShellUtils.runCommand(project, "./gradlew", "clean");
    }

    private void removeNonExistentFiles() {
        scans.values().forEach(scan -> scan.files.removeIf(file -> !file.toFile().exists()));
    }

    private Path getSrcDirFromTaskname(CorpusProject project, String taskName) {
        // taskName will be of form :target1:...:targetK:compileJava or :target1:...:targetK:compileTestJava
        // :target1:...:targetK:compileJava -----> target1/.../targetK/src/main
        // :target1:...:targetK:compileTestJava ------> target1/.../targetK/src/test
        List<String> taskParts = Arrays.asList(taskName.substring(1).split(":"));

        String projRelativePath = Joiner.on('/').join(taskParts.subList(0, taskParts.size() - 1));
        String filesDir = Iterables.getLast(taskParts).equals("compileJava") ? "src/main" : "src/test";

        return project.getRoot().resolve(projRelativePath).resolve(filesDir);
    }

    private Optional<List<ProjectFile>> getFilesFromTaskName(CorpusProject project, String taskName) {
        Path projectSrcDir = getSrcDirFromTaskname(project, taskName);

        if (!projectSrcDir.toFile().exists()) {
            return Optional.empty();
        }

        return Optional.of(FileUtils.findFilesMatchingGlob(projectSrcDir, "**/*.java").stream()
                .filter(project::shouldScanFile)
                .map(file -> new ProjectFile(project, file))
                .collect(Collectors.toList()));
    }

    private void updateDiagnosticsFromScript(CorpusProject project) throws IOException, InterruptedException {
        String scriptOutput = ShellUtils.runCommand(project.getRoot().toFile(), "/usr/bin/python3.8",
                "/home/monty/IdeaProjects/error-prone/core/src/test/java/com/google/errorprone/bugtrack/harness/get_gradle_cmdargs.py",
                project.getRoot().toString());

        if (scriptOutput.trim().isEmpty()) {
            return;
        }

        String[] scriptOutputLines = scriptOutput.split("\n");
        for (int i = 0; i < scriptOutputLines.length; i += 2) {
            String scanName = scriptOutputLines[i];

            Optional<List<ProjectFile>> filesToParse = getFilesFromTaskName(project, scanName);
            if (filesToParse.isPresent()) {
                scans.put(scanName, new DiagnosticsScan(
                        scanName,
                        filesToParse.get(),
                        filterCmdLineArgs(scriptOutputLines[i + 1])));
            }
        }
    }

    @Override
    public Collection<DiagnosticsScan> getScans(CorpusProject project) throws IOException, InterruptedException {
        updateDiagnosticsFromScript(project);
        removeNonExistentFiles();

        // Deep copy to ensure any changes don't mutate our internal copies.
        return scans.values().stream().map(DiagnosticsScan::new).collect(Collectors.toList());
    }
}
