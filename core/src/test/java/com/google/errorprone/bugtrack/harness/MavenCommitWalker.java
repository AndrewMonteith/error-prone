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

import com.google.common.collect.ImmutableSet;
import com.google.common.io.Files;
import com.google.errorprone.bugtrack.projects.CorpusProject;
import com.google.errorprone.bugtrack.projects.ProjectFile;
import com.google.errorprone.bugtrack.projects.RootAlternatingProject;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.revwalk.RevCommit;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

import static com.google.errorprone.bugtrack.projects.ShouldScanUtils.isJavaFile;

public final class MavenCommitWalker implements Iterator<Collection<DiagnosticsScan>>, Iterable<Collection<DiagnosticsScan>> {
    private final RootAlternatingProject project;
    private final Iterator<RevCommit> commits;
    private final Map<String, DiagnosticsScan> currentScans;

    private boolean firstCall = true;

    public MavenCommitWalker(CorpusProject project, Iterable<RevCommit> commits) throws IOException {
        this.project = new RootAlternatingProject(project);
        this.commits = commits.iterator();
        this.currentScans = new HashMap<>();
    }

    private static List<String> parseCmdLineArguments(String rawCmdLineArgs) {
        List<String> args = new ArrayList<>();

        Set<String> singleArgBlockList = ImmutableSet.of("-nowarn", "-deprecation");

        String[] individualArgs = rawCmdLineArgs.split(" ");
        for (int i = 0; i < individualArgs.length; ++i) {
            if (singleArgBlockList.contains(individualArgs[i])) { continue; }
            else if (individualArgs[i].equals("-d")) { ++i; continue; }
            else if (isJavaFile(individualArgs[i])) { continue; }
            else if (individualArgs[i].equals("1.5")) { individualArgs[i] = "1.7"; }
            else if (individualArgs[i].equals("1.6")) { individualArgs[i] = "1.7"; }
            else if (individualArgs[i].startsWith("-Xlint")) { continue; }
            else if (individualArgs[i].startsWith("-Xdoclint")) { continue; }

            args.add(individualArgs[i]);
        }

        return args;
    }

    @Override
    public boolean hasNext() {
        return commits.hasNext();
    }

    @Override
    public Collection<DiagnosticsScan> next() {
        try {
            project.switchDir();
            RevCommit commit = commits.next();
            new Git(project.loadRepo()).checkout().setName(commit.getName()).call();
            updateCurrentDiagnosticsScan();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return currentScans.values();
    }

    private void cleanProject() {
        try {
            ShellUtils.runCommand(new File(project.getRoot()), "mvn", "clean");
            ShellUtils.runCommand(project.getOtherDir().toFile(), "mvn", "clean");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private List<ProjectFile> findFilesToScan(String sourcepath) {
        return Arrays.stream(sourcepath.split(":"))
                .map(path -> FileUtils.findFilesMatchingGlob(Paths.get(path), "**/*.java"))
                .flatMap(Collection::stream)
                .filter(project::shouldScanFile)
                .map(file -> new ProjectFile(project, file))
                .collect(Collectors.toList());
    }

    private void updateCurrentDiagnosticsScan() throws IOException, InterruptedException {
        if (firstCall) {
            cleanProject();
            firstCall = false;
        }

        String scriptOutput = ShellUtils.runCommand(new File(project.getRoot()),
                "/usr/bin/python3.8",
                "/home/monty/IdeaProjects/error-prone/core/src/test/java/com/google/errorprone/bugtrack/harness/get_maven_cmdargs.py",
                project.getRoot());

        String[] scriptOutputLines = scriptOutput.split("\n");
        for (int i = 0; i < scriptOutputLines.length; i += 2) {
            String scanName = scriptOutputLines[i];
            List<String> cmdLineArguments = parseCmdLineArguments(scriptOutputLines[i + 1]);
            List<ProjectFile> filesToParse = findFilesToScan(
                    cmdLineArguments.get(cmdLineArguments.indexOf("-sourcepath") + 1));

            currentScans.put(scanName, new DiagnosticsScan(
                    scanName,
                    filesToParse,
                    cmdLineArguments));
        }
    }

    @Override
    public Iterator<Collection<DiagnosticsScan>> iterator() {
        return this;
    }
}
