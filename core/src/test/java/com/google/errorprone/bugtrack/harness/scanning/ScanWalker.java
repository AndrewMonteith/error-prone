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

import com.google.errorprone.bugtrack.utils.ShellUtils;
import com.google.errorprone.bugtrack.projects.CorpusProject;
import com.google.errorprone.bugtrack.projects.ProjectFile;
import com.google.errorprone.bugtrack.utils.ProjectFiles;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

public final class ScanWalker implements Iterable<Collection<DiagnosticsScan>>, Iterator<Collection<DiagnosticsScan>> {
    private final CorpusProject project;
    private final Iterator<RevCommit> commits;
    private final ProjectScanner projectScanner;

    public ScanWalker(CorpusProject project, Iterable<RevCommit> commits, ProjectScanner scanner) {
        this.project = project;
        this.commits = commits.iterator();
        this.projectScanner = scanner;
    }

    public ScanWalker(CorpusProject project, Iterable<RevCommit> commits) {
        this.project = project;
        this.commits = commits.iterator();

        switch(project.getBuildSystem()) {
            case Maven:
                this.projectScanner = new MavenProjectScanner();
                break;
            case Gradle:
                this.projectScanner = new GradleProjectScanner();
                break;
            default:
                throw new IllegalArgumentException("not yet supporting build system of project " + project.getRoot());
        }
    }

    private void cleanProject() {
        try {
            projectScanner.cleanProject(project.getRoot());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean hasNext() {
        return commits.hasNext();
    }

    private void sanitiseFilesInScans(Collection<DiagnosticsScan> scans) throws IOException, InterruptedException {
        List<String> cmdToRun = scans.stream()
                .map(scan -> scan.files)
                .flatMap(Collection::stream)
                .map(ProjectFile::toString).collect(Collectors.toList());

        cmdToRun.add(0, ProjectFiles.find("error-prone", "sanitise_files.sh").toString());
        cmdToRun.add(0, "/bin/bash");

        ShellUtils.runCommand(
                project.getRoot(),
                cmdToRun.toArray(new String[0]));
    }

    @Override
    public Collection<DiagnosticsScan> next() {
        try {
            System.out.println("Cleaning project");
            // Clean all stuff from the build cache
            cleanProject();

            System.out.println("forwarding repo");
            // Forward to the next commit
//            ShellUtils.runCommand(project.getRoot(), "git", "checkout", ".");
//            ShellUtils.runCommand(project.getRoot(), "git", "checkout", commits.next().getName());
            Repository repo = project.loadRepo();
            new Git(repo).checkout().setAllPaths(true).call();
            new Git(repo).checkout().setName(commits.next().getName()).call();

            System.out.println("running shell cmds");
            // Normalize the whitespace in all files
            ShellUtils.runCommand(project.getRoot(),
                ProjectFiles.find("error-prone", "sanitise_project.sh").toString());

            // Collect the scans
            System.out.println("scanning");
            Collection<DiagnosticsScan> scans = projectScanner.getScans(project);

            System.out.println("sanitising scans");
            sanitiseFilesInScans(scans);

            return scans;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return Collections.emptyList();
    }

    @Override
    public Iterator<Collection<DiagnosticsScan>> iterator() {
        return this;
    }
}
