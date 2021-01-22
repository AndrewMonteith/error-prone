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
import com.google.errorprone.bugtrack.BugComparer;
import com.google.errorprone.bugtrack.CommitRange;
import com.google.errorprone.bugtrack.GitUtils;
import com.google.errorprone.bugtrack.CollectionUtil;
import com.google.errorprone.bugtrack.projects.CorpusProject;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.openjdk.tools.javac.util.Pair;

import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import java.io.IOException;
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
            System.out.println("Loaded diagnostic scan information");
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
        Collection<Diagnostic<? extends JavaFileObject>> oldDiagnostics = new ArrayList<>();
        Collection<Diagnostic<? extends JavaFileObject>> newDiagnostics = new ArrayList<>();

        forEachCommitWithDiagnostics(ImmutableList.of(oldCommit, newCommit), (commit, diagnostics) -> {
            if (commit == oldCommit) {
                oldDiagnostics.addAll(diagnostics);
            } else {
                newDiagnostics.addAll(diagnostics);
            }
        });

        Map<Diagnostic<? extends JavaFileObject>, Diagnostic<? extends JavaFileObject>> matchedDiagnostics = new HashMap<>();

        oldDiagnostics.forEach(oldDiagnostic -> {
            Collection<Diagnostic<? extends JavaFileObject>> matching = CollectionUtil.filter(
                    newDiagnostics, newDiagnostic -> comparer.areSame(oldDiagnostic, newDiagnostic));

            if (matching.size() == 1) {
                matchedDiagnostics.put(oldDiagnostic, Iterables.getOnlyElement(matching));
            } else if (matching.size() > 1) {
                comparer.breakTies(oldDiagnostic, matching)
                        .ifPresent(matchingDiagnostic -> matchedDiagnostics.put(oldDiagnostic, matchingDiagnostic));
            }
        });

        printMatchResults(oldDiagnostics, newDiagnostics, matchedDiagnostics);
    }

    private void printMatchResults(Collection<Diagnostic<? extends JavaFileObject>> oldDiagnostics,
                                   Collection<Diagnostic<? extends JavaFileObject>> newDiagnostics,
                                   Map<Diagnostic<? extends JavaFileObject>, Diagnostic<? extends JavaFileObject>> matches) {
        Set<Diagnostic<? extends JavaFileObject>> unmatchedOld = Sets.difference(
                Sets.newHashSet(oldDiagnostics), Sets.newHashSet(matches.keySet()));

        Set<Diagnostic<? extends JavaFileObject>> unmatchedNew = Sets.difference(
                Sets.newHashSet(newDiagnostics), Sets.newHashSet(matches.values()));

        System.out.printf("There are %d old and %d new diagnostics\n", oldDiagnostics.size(), newDiagnostics.size());
        System.out.printf("We matched %d old diagnostics\n", matches.keySet().size());
        System.out.printf("We could not match %d old and %d new diagnostics\n", unmatchedOld.size(), unmatchedNew.size());

        if (!unmatchedOld.isEmpty()) {
            System.out.println("Unmatched old diagnostics:");
            unmatchedOld.forEach(System.out::println);
            System.out.println();
        }

        System.out.println("Unmatched new diagnostics:");
        unmatchedNew.forEach(System.out::println);
    }

    public void findInterestingPairs(CommitRange range) throws GitAPIException {
        List<RevCommit> commits = GitUtils.expandCommitRange(project.loadRepo(), range);

    }
}
