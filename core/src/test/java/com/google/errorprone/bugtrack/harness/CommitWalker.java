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
import com.google.errorprone.bugtrack.projects.RootAlternatingProject;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.revwalk.RevCommit;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

public final class CommitWalker implements Iterable<Collection<DiagnosticsScan>>, Iterator<Collection<DiagnosticsScan>> {
    private final CorpusProject project;
    private final Iterator<RevCommit> commits;
    private final ProjectScanner projectScanner;

    public CommitWalker(CorpusProject project, Iterable<RevCommit> commits, ProjectScanner scanner) {
        this.project = project;
        this.commits = commits.iterator();
        this.projectScanner = scanner;
    }

    private void cleanProject() {
        try {
            projectScanner.cleanProject(project.getRoot().toFile());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean hasNext() {
        return commits.hasNext();
    }

    @Override
    public Collection<DiagnosticsScan> next() {
        try {
            cleanProject();
            RevCommit commit = commits.next();
            new Git(project.loadRepo()).checkout().setName(commit.getName()).call();
            return projectScanner.getScans(project);
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
