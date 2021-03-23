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

package com.google.errorprone.bugtrack.harness.matching;

import com.google.errorprone.bugtrack.BugComparer;
import com.google.errorprone.bugtrack.GitSrcFilePairLoader;
import com.google.errorprone.bugtrack.SrcFilePairLoader;
import com.google.errorprone.bugtrack.harness.DiagnosticsFile;
import com.google.errorprone.bugtrack.motion.DiagnosticPositionMotionComparer;
import com.google.errorprone.bugtrack.motion.ExactDiagnosticMatcher;
import com.google.errorprone.bugtrack.motion.trackers.DiagnosticPositionTrackerConstructor;
import com.google.errorprone.bugtrack.projects.CorpusProject;
import org.eclipse.jgit.api.errors.GitAPIException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public final class GitCommitMatcher {
    private final CorpusProject project;
    private final DiagnosticsFile oldDiagFile;
    private final DiagnosticsFile newDiagFile;
    private final SrcFilePairLoader srcFilePairLoader;

    private final List<BugComparer> comparers;

    private GitCommitMatcher(CorpusProject project,
                             DiagnosticsFile oldDiagFile,
                             DiagnosticsFile newDiagFile,
                             SrcFilePairLoader srcFilePairLoader) {
        this.project = project;
        this.oldDiagFile = oldDiagFile;
        this.newDiagFile = newDiagFile;
        this.srcFilePairLoader = srcFilePairLoader;
        this.comparers = new ArrayList<>();
    }

    public static GitCommitMatcher compareGit(CorpusProject project,
                                              DiagnosticsFile oldDiagFile,
                                              DiagnosticsFile newDiagFile) throws IOException {
        return new GitCommitMatcher(project,
                oldDiagFile,
                newDiagFile,
                new GitSrcFilePairLoader(project.loadRepo(), oldDiagFile.commitId, newDiagFile.commitId));
    }

    public GitCommitMatcher trackPosition(DiagnosticPositionTrackerConstructor posCtor) throws IOException {
        this.comparers.add(new DiagnosticPositionMotionComparer(srcFilePairLoader, posCtor));
        return this;
    }

    public GitCommitMatcher trackIdentical() {
        this.comparers.add(new ExactDiagnosticMatcher());
        return this;
    }

    public MatchResults match() throws IOException, GitAPIException {
        MatchResults results = DiagnosticsMatcher.fromFiles(
                project,
                oldDiagFile,
                newDiagFile,
                BugComparer.any(comparers.toArray(new BugComparer[0]))).getResults();

        System.gc();
        return results;
    }

    public GitCommitMatcher track(BugComparer comparer) {
        this.comparers.add(comparer);
        return this;
    }
}
