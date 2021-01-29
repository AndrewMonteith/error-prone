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

import com.google.common.collect.*;
import com.google.errorprone.bugtrack.*;
import com.google.errorprone.bugtrack.harness.scanning.DiagnosticsCollector;
import com.google.errorprone.bugtrack.harness.scanning.DiagnosticsScan;
import com.google.errorprone.bugtrack.harness.scanning.GradleProjectScanner;
import com.google.errorprone.bugtrack.harness.scanning.MavenProjectScanner;
import com.google.errorprone.bugtrack.projects.CorpusProject;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.openjdk.tools.javac.util.Pair;

import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

public final class ProjectHarness {
    private final CorpusProject project;
    private boolean verbose = false;

    public ProjectHarness(CorpusProject project) {
        this.project = project;
    }

    public ProjectHarness(CorpusProject project, boolean verbose) {
        this.project = project;
        this.verbose = verbose;
    }

    public Collection<Diagnostic<? extends JavaFileObject>> collectDiagnostics(String commitHash) throws IOException {
        return collectDiagnostics(GitUtils.parseCommit(project.loadRepo(), commitHash));
    }

    public Collection<Diagnostic<? extends JavaFileObject>> collectDiagnostics(RevCommit commit) throws IOException {
        Collection<DiagnosticsScan> diagnosticScans = Iterables.getLast(loadScanWalker(ImmutableList.of(commit)));
        return DiagnosticsCollector.collectDiagnostics(diagnosticScans, verbose);
    }

    private Iterable<Collection<DiagnosticsScan>> loadScanWalker(Iterable<RevCommit> commits) throws IOException {
        switch (project.getBuildSystem()) {
            case Maven:
                return new CommitWalker(project, commits, new MavenProjectScanner());
            case Gradle:
                return new CommitWalker(project, commits, new GradleProjectScanner());
            default:
                throw new IllegalArgumentException("not yet supporting build system of project " + project.getRoot());
        }
    }

    public void walkCommitRange(CommitRange range) throws IOException, GitAPIException {
        List<RevCommit> commits = GitUtils.expandCommitRange(project.loadRepo(), range);
        System.out.printf("Going to scan %d commits\n", commits.size());

        final List<DiagnosticsDistribution> distributions = new ArrayList<>();

        forEachCommitWithDiagnostics(commits, (commit, diagnostics) -> {
            System.out.printf("Commit %s had %d alerts\n", commit.getName(), diagnostics.size());

            DiagnosticsDistribution distribution = DiagnosticsDistribution.fromDiagnostics(diagnostics);
            if (!distributions.isEmpty() && !Iterables.getLast(distributions).equals(distribution)) {
                // The distribution has changed, let's tell them
                System.out.println("Diagnostics distribution has changed since last!");
            }
            distributions.add(distribution);
        });
    }

    public void forEachCommitWithDiagnostics(Iterable<RevCommit> commits,
                                             BiConsumer<RevCommit, Collection<Diagnostic<? extends JavaFileObject>>> consumer) throws IOException {
        Iterable<Collection<DiagnosticsScan>> scanWalker = loadScanWalker(commits);

        if (verbose) {
            System.out.println("Loading diagnostic scan information");
        }

        Streams.zip(Streams.stream(commits), Streams.stream(scanWalker), Pair::of).forEach(commitToScan -> {
            Collection<Diagnostic<? extends JavaFileObject>> diagnostics = new ArrayList<>();

            try {
                diagnostics.addAll(DiagnosticsCollector.collectDiagnostics(commitToScan.snd, verbose));
            } catch (AssertionError e) {
                System.out.println("Failed to collect diagnostics for commit " + commitToScan.fst.getName());
            }

            consumer.accept(commitToScan.fst, diagnostics);
        });
    }

    public void forEachCommitIdWithDiagnostics(Iterable<String> commits,
                                               BiConsumer<RevCommit, Collection<Diagnostic<? extends JavaFileObject>>> consumer) throws IOException {
        forEachCommitWithDiagnostics(Streams.stream(commits)
                .map(commitId -> {
                    try {
                        return GitUtils.parseCommit(project.loadRepo(), commitId);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    return null;
                }).collect(Collectors.toList()), consumer);
    }

    public void compareTwoCommits(String oldCommitId, String newCommitId, BugComparer comparer) throws IOException {
        Repository repo = project.loadRepo();
        compareTwoCommits(
                GitUtils.parseCommit(repo, oldCommitId),
                GitUtils.parseCommit(repo, newCommitId),
                comparer
        );
    }

    public void compareTwoCommits(RevCommit oldCommit, RevCommit newCommit, BugComparer comparer) throws IOException {
        Collection<DatasetDiagnostic> oldDiagnostics = new ArrayList<>();
        Collection<DatasetDiagnostic> newDiagnostics = new ArrayList<>();

        forEachCommitWithDiagnostics(ImmutableList.of(oldCommit, newCommit), (commit, diagnostics) -> {
            Collection<DatasetDiagnostic> sink = (commit == oldCommit) ? oldDiagnostics : newDiagnostics;

            Iterables.transform(diagnostics, DatasetDiagnostic::new).forEach(sink::add);
        });

        System.out.println(computeMatches(oldDiagnostics, newDiagnostics, comparer));
    }

    public MatchResults computeMatches(Collection<DatasetDiagnostic> oldDiagnostics,
                                       Collection<DatasetDiagnostic> newDiagnostics,
                                       BugComparer comparer) {
        Map<DatasetDiagnostic, DatasetDiagnostic> matchedDiagnostics = new HashMap<>();

        oldDiagnostics.forEach(oldDiagnostic -> {
            Iterable<DatasetDiagnostic> matching = Iterables.filter(newDiagnostics,
                    newDiagnostic -> !matchedDiagnostics.containsValue(newDiagnostic) && comparer.areSame(oldDiagnostic, newDiagnostic));

            if (Iterables.size(matching) == 1) {
                matchedDiagnostics.put(oldDiagnostic, Iterables.getOnlyElement(matching));
            } else if (Iterables.size(matching) > 1) {
                comparer.breakTies(oldDiagnostic, matching)
                        .ifPresent(matchingDiagnostic -> matchedDiagnostics.put(oldDiagnostic, matchingDiagnostic));
            }
        });

        return new MatchResults(oldDiagnostics, newDiagnostics, matchedDiagnostics);
    }

    public void serialiseCommit(String commit, String output) throws IOException {
        serialiseCommit(GitUtils.parseCommit(project.loadRepo(), commit), output);
    }

    public void serialiseCommit(RevCommit commit, String output) throws IOException {
        Collection<Diagnostic<? extends JavaFileObject>> diagnostics = collectDiagnostics(commit);

        System.out.println(diagnostics.size());

        StringBuilder fileOutput = new StringBuilder();
        fileOutput.append(commit.getName()).append(" ").append(diagnostics.size()).append("\n");
        diagnostics.forEach(diagnostic -> fileOutput.append(new DatasetDiagnostic(diagnostic)));

        Files.write(Paths.get(output), fileOutput.toString().getBytes());
    }

    public void serialiseCommits(CommitRange range, CommitRangeFilter filter, String output) throws GitAPIException, IOException {
        List<RevCommit> commits = filter.filterCommits(GitUtils.expandCommitRange(project.loadRepo(), range));
        Path outputDir = Paths.get(output);
        if (!outputDir.toFile().isDirectory()) {
            throw new RuntimeException(output + " is not a directory.");
        }

        int commitNum = 0;
        for (RevCommit commit : commits) {
            Path diagnosticsOutput = outputDir.resolve(commitNum + " " + commit.getName());
            try {
                serialiseCommit(commit, diagnosticsOutput.toString());
            } catch (Exception e) {
                try {
                    Files.write(diagnosticsOutput, Arrays.asList("failed to write", Arrays.toString(e.getStackTrace())));
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                }
            }

            ++commitNum;
        }
    }

    public void compareDiagnosticsFile(DatasetDiagnosticsFile oldDiagnostics, DatasetDiagnosticsFile newDiagnostics, BugComparer comparer) {
        System.out.println(computeMatches(oldDiagnostics.diagnostics, newDiagnostics.diagnostics, comparer));
    }
}
