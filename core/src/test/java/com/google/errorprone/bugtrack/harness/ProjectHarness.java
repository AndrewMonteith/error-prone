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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.errorprone.bugtrack.*;
import com.google.errorprone.bugtrack.harness.matching.DiagnosticsMatcher;
import com.google.errorprone.bugtrack.harness.scanning.DiagnosticsCollector;
import com.google.errorprone.bugtrack.projects.CorpusProject;
import com.google.errorprone.bugtrack.utils.GitUtils;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;

import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.BiConsumer;

public final class ProjectHarness {
    private final CorpusProject project;
    private Verbosity verbose = Verbosity.SILENT;

    public ProjectHarness(CorpusProject project) {
        this.project = project;
    }

    public ProjectHarness(CorpusProject project, Verbosity verbose) {
        this.project = project;
        this.verbose = verbose;
    }

    private void forEachCommitWithDiagnostics(Iterable<RevCommit> commits,
                                             BiConsumer<RevCommit, Collection<Diagnostic<? extends JavaFileObject>>> consumer) throws IOException {
        if (verbose == Verbosity.VERBOSE) {
            System.out.println("Loading diagnostic scan information");
        }

        commits.forEach(commit -> {
            Collection<Diagnostic<? extends JavaFileObject>> diagnostics = new ArrayList<>();

            try {
                diagnostics.addAll(DiagnosticsCollector.collectEPDiagnostics(project, commit, verbose));
            } catch (Exception e) {
                System.out.println("Failed to collect diagnostics for commit " + commit.getName());
            }

            consumer.accept(commit, diagnostics);
        });
    }

    public void forEachCommitIdWithDiagnostics(Iterable<String> commitHashes,
                                               BiConsumer<RevCommit, Collection<Diagnostic<? extends JavaFileObject>>> consumer) throws IOException {
        Iterable<RevCommit> commits = Iterables.transform(commitHashes, commitId -> {
            try {
                return GitUtils.parseCommit(project.loadRepo(), commitId);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        });

        forEachCommitWithDiagnostics(commits, consumer);
    }

    public void compareTwoCommits(String oldCommitId, String newCommitId, PathsComparer pathsComparer, BugComparer comparer) throws IOException {
        Repository repo = project.loadRepo();
        compareTwoCommits(
                GitUtils.parseCommit(repo, oldCommitId),
                GitUtils.parseCommit(repo, newCommitId),
                pathsComparer,
                comparer
        );
    }

    public void compareTwoCommits(RevCommit oldCommit, RevCommit newCommit, PathsComparer pathComparer, BugComparer bugComparer) throws IOException {
        Collection<DatasetDiagnostic> oldDiagnostics = new ArrayList<>();
        Collection<DatasetDiagnostic> newDiagnostics = new ArrayList<>();

        forEachCommitWithDiagnostics(ImmutableList.of(oldCommit, newCommit), (commit, diagnostics) -> {
            Collection<DatasetDiagnostic> sink = (commit == oldCommit) ? oldDiagnostics : newDiagnostics;

            Iterables.transform(diagnostics, DatasetDiagnostic::new).forEach(sink::add);
        });

        System.out.println(new DiagnosticsMatcher(oldDiagnostics, newDiagnostics, bugComparer, pathComparer).getResults());
    }

    public void serialiseCommit(RevCommit commit, Path output) throws IOException {
//        GitUtils.checkoutFiles(project.loadRepo());

        Collection<Diagnostic<? extends JavaFileObject>> diagnostics =
                DiagnosticsCollector.collectEPDiagnostics(project, commit, verbose);

        DiagnosticsFile.save(output, commit, diagnostics);
    }

    public void serialiseCommits(CommitRange range, CommitRangeFilter filter, Path output) throws GitAPIException, IOException, InterruptedException {
        serialiseCommits(range, filter, output, 0);
    }

    public void serialiseCommits(CommitRange range, CommitRangeFilter filter, Path output, int commitNum) throws GitAPIException, IOException, InterruptedException {
        if (!output.toFile().isDirectory()) {
            throw new RuntimeException(output + " is not a directory.");
        }

        List<RevCommit> commits = filter.filterCommits(GitUtils.expandCommitRange(project.loadRepo(), range));

        for (; commitNum < commits.size(); ++commitNum) {
            RevCommit commit = commits.get(commitNum);
            System.out.printf("Serialising commit %s [%d / %d]\n", commits.get(commitNum).getName(), commitNum+1, commits.size());
            Path diagnosticsOutput = output.resolve(commitNum + " " + commit.getName());
            try {
                serialiseCommit(commit, diagnosticsOutput);
            } catch (Throwable e) {
                try {
                    Files.write(diagnosticsOutput, Arrays.asList("failed to write", Arrays.toString(e.getStackTrace())));
                } catch (IOException e2) {
                    e2.printStackTrace();
                }
            }
        }
    }
}
