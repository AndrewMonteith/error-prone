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

package com.google.errorprone.bugtrack.harness.evaluating;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.errorprone.bugtrack.harness.DiagnosticsFile;
import com.google.errorprone.bugtrack.harness.matching.GitCommitMatcher;
import com.google.errorprone.bugtrack.harness.matching.MatchResults;
import com.google.errorprone.bugtrack.projects.CorpusProject;
import com.google.errorprone.bugtrack.util.ThrowingBiConsumer;
import com.google.errorprone.bugtrack.util.ThrowingConsumer;
import org.eclipse.jgit.api.errors.GitAPIException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.BiConsumer;

import static com.google.errorprone.bugtrack.harness.utils.ListUtils.consecutivePairs;
import static com.google.errorprone.bugtrack.motion.trackers.DPTrackerConstructorFactory.*;

public final class MultiGrainDiagFileComparer {
    private final CorpusProject project;
    private final Map<CommitPair, MatchResults> resultsCache;

    private MultiGrainDiagFileComparer(CorpusProject project) {
        this.project = project;
        this.resultsCache = new ConcurrentHashMap<>();
    }

    public static void compareFiles(CorpusProject project,
                                    Path output,
                                    List<GrainDiagFile> grainFiles) {
        new MultiGrainDiagFileComparer(project).compare(output, grainFiles);
    }

    public static void compareFilesInParallel(CorpusProject project,
                                              Path output,
                                              List<GrainDiagFile> grainFiles) {
        new MultiGrainDiagFileComparer(project).compareParallel(output, grainFiles);
    }

    private MatchResults scan(DiagnosticsFile last, DiagnosticsFile next) throws IOException, GitAPIException {
        CommitPair cp = new CommitPair(last.commitId, next.commitId);
        if (resultsCache.containsKey(cp)) {
            System.out.println("retrieved " + cp + " from cache");
            return resultsCache.get(cp);
        }

        MatchResults results;
        try {
            results = GitCommitMatcher.compareGit(project, last, next)
                    .trackIdentical()
                    .trackPosition(any(newTokenizedLineTracker(), newIJMStartAndEndTracker()))
                    .match();
        } catch (RuntimeException e) {
            System.out.printf("Failed scanning %s -> %s\n", last.commitId, next.commitId);
            throw new RuntimeException(e);
        }

        resultsCache.put(cp, results);

        return results;
    }

    private void scanWalk(Path output, Iterable<GrainDiagFile> grainFiles) {
        consecutivePairs(grainFiles, (ThrowingBiConsumer<GrainDiagFile, GrainDiagFile>) (oldGrainFile, newGrainFile) -> {
            DiagnosticsFile last = oldGrainFile.getDiagFile();
            DiagnosticsFile next = newGrainFile.getDiagFile();

            if (last.commitId.equals(next.commitId)) {
                return;
            }

            String name = last.getSeqNum() + " " + last.commitId + " -> " + next.getSeqNum() + " " + next.commitId;
            System.out.println("Scanning " + name);

            scan(last, next).save(output.resolve(name));
        });
    }

    private ImmutableSet<Integer> getAllGrains(List<GrainDiagFile> grainFiles) {
        return grainFiles.stream()
                .map(GrainDiagFile::getGrains)
                .reduce(ImmutableSet.of(), (s1, s2) ->
                        ImmutableSet.<Integer>builder()
                                .addAll(s1)
                                .addAll(s2)
                                .build()
                );
    }

    private Iterable<GrainDiagFile> filterGrainFiles(Iterable<GrainDiagFile> grainFiles,
                                                     final int maxGrain,
                                                     final int grain) {
        return Iterables.filter(grainFiles, grainFile -> grainFile.hasGrain(maxGrain) || grainFile.hasGrain(grain));
    }

    private void compare(Path output, List<GrainDiagFile> grainFiles) {
        Set<Integer> grains = getAllGrains(grainFiles);

        makeGrainFolders(output, grains);

        final int maxGrain = Collections.max(grains);

        for (int grain : grains) {
            System.out.println("scanning grain " + grain);
            scanWalk(output.resolve("grain-" + grain), filterGrainFiles(grainFiles, maxGrain, grain));
        }
    }

    private void makeGrainFolders(Path output, Iterable<Integer> grains) {
        for (int grain : grains) {
            Path grainOutput = output.resolve("grain-" + grain);
            if (!grainOutput.toFile().exists()) {
                try {
                    Files.createDirectory(grainOutput);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    private void compareParallel(Path output, List<GrainDiagFile> grainFiles) {
        Set<Integer> grains = getAllGrains(grainFiles);

        makeGrainFolders(output, grains);

        final int maxGrain = Collections.max(grains);

        // Create task for each pair of files. Record which grains need the results of the comparisons
        Map<CommitPair, CompareTask> commitPairsTasks = new HashMap<>();
        for (int grain : grains) {
            Path grainOutput = output.resolve("grain-" + grain);
            consecutivePairs(filterGrainFiles(grainFiles, maxGrain, grain), (oldGrainFile, newGrainFile) -> {
                DiagnosticsFile oldFile = oldGrainFile.getDiagFile();
                DiagnosticsFile newFile = newGrainFile.getDiagFile();

                String fileName = oldFile.getSeqNum() + " " + oldFile.commitId + " -> "
                        + newFile.getSeqNum() + " " + newFile.commitId;

                commitPairsTasks.computeIfAbsent(
                        new CommitPair(oldFile.commitId, newFile.commitId),
                        __ -> new CompareTask(oldFile, newFile, new ArrayList<>()))
                        .outputs.add(grainOutput.resolve(fileName));
            });
        }

        final int processors = Runtime.getRuntime().availableProcessors();
        final int grainSize = commitPairsTasks.values().size() / processors;

        // Assign all tasks equally to each core available
        List<Callable<Void>> compareThreadTasks = new ArrayList<>();
        Lists.partition(ImmutableList.copyOf(commitPairsTasks.values()), grainSize)
                .forEach(compareTasks ->
                        compareThreadTasks.add(() -> {
                            long threadId = Thread.currentThread().getId();
                            for (int i = 0; i < compareTasks.size(); ++i) {
                                System.out.printf("Thread %d [%d / %d]\n", threadId, i + 1, compareTasks.size());

                                CompareTask task = compareTasks.get(i);
                                MatchResults results = scan(task.before, task.after);
                                task.outputs.forEach((ThrowingConsumer<Path>) results::save);
                            }

                            return null;
                        }));

        // Spin up the threads
        ExecutorService executor = new ForkJoinPool();
        try {
            // Let's go!
            List<Future<Void>> results = executor.invokeAll(compareThreadTasks);
            for (Future<Void> future : results) {
                future.get();
            }
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        } finally {
            executor.shutdown();
        }
    }

    private static class CompareTask {
        public final DiagnosticsFile before;
        public final DiagnosticsFile after;
        public final List<Path> outputs;

        public CompareTask(final DiagnosticsFile before, final DiagnosticsFile after, final List<Path> outputs) {
            this.before = before;
            this.after = after;
            this.outputs = outputs;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            CompareTask that = (CompareTask) o;
            return before.equals(that.before) && after.equals(that.after) && Iterables.elementsEqual(outputs, that.outputs);
        }

        @Override
        public int hashCode() {
            return Objects.hash(before, after, outputs.size());
        }

        @Override
        public String toString() {
            return before.commitId + " -> " + after.commitId;
        }
    }

    private static class CommitPair {
        public final String preCommit;
        public final String postCommit;

        public CommitPair(String preCommit, String postCommit) {
            this.preCommit = preCommit;
            this.postCommit = postCommit;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            CommitPair that = (CommitPair) o;
            return preCommit.equals(that.preCommit) && postCommit.equals(that.postCommit);
        }

        @Override
        public int hashCode() {
            return Objects.hash(preCommit, postCommit);
        }

        @Override
        public String toString() {
            return preCommit + " ->" + postCommit;
        }
    }
}
