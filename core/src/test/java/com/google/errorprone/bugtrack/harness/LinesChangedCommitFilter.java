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
import com.google.common.collect.Lists;
import com.google.common.io.Files;
import com.google.errorprone.bugtrack.util.ThrowingFunction;
import com.google.errorprone.bugtrack.utils.GitUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.patch.FileHeader;
import org.eclipse.jgit.revwalk.RevCommit;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import static com.google.errorprone.bugtrack.harness.utils.ListUtils.consecutivePairs;
import static com.google.errorprone.bugtrack.harness.utils.ListUtils.enumerate;

public class LinesChangedCommitFilter implements CommitRangeFilter {
    private final int javaLinesChangedThreshold;
    private final Git git;

    public LinesChangedCommitFilter(Git git, int javaLinesChangedThreshold) {
        this.javaLinesChangedThreshold = javaLinesChangedThreshold;
        this.git = git;
    }

    private static int computeJavaLinesChanged(Git git, RevCommit earlierCommit, RevCommit laterCommit) throws IOException, GitAPIException {
        List<DiffEntry> diffs = GitUtils.computeDiffs(git, earlierCommit, laterCommit);

        DiffFormatter formatter = new DiffFormatter(null);
        formatter.setRepository(git.getRepository());
        formatter.setDetectRenames(true);

        int totalLinesChanged = 0;
        for (DiffEntry diff : diffs) {
            if (!Files.getFileExtension(diff.getNewPath()).equals("java")) {
                continue;
            }

            FileHeader header = formatter.toFileHeader(diff);

            totalLinesChanged += header.toEditList().stream()
                    .mapToInt(edit -> edit.getLengthA() + edit.getLengthB())
                    .sum();
        }

        return totalLinesChanged;
    }

    @Override
    public List<RevCommit> filterCommits(List<RevCommit> commits) throws IOException, GitAPIException {
        return filterCommitsParallel(commits);
    }

    private int computeGrainSize(final int compareTasks) {
        final int processors = Runtime.getRuntime().availableProcessors();

        if (compareTasks / processors == 0) {
            return 4;
        } else {
            return Math.max(compareTasks / processors / 4, 1);
        }
    }

    public List<RevCommit> filterCommitsParallel(List<RevCommit> commits) {
        List<CompareTask> compareTasks = new ArrayList<>();
        consecutivePairs(commits, (oldCommit, newCommit) -> compareTasks.add(new CompareTask(git, oldCommit, newCommit)));

        final int grainSize = computeGrainSize(compareTasks.size());

        // Split pairs evenly across all processors
        List<Callable<CompareResult>> compareThreadTasks = new ArrayList<>();
        enumerate(Lists.partition(compareTasks, grainSize), (chunkId, tasks) ->
                compareThreadTasks.add(() -> new CompareResult(chunkId, tasks.stream()
                        .map((ThrowingFunction<CompareTask, Integer>) CompareTask::computeDiff)
                        .collect(ImmutableList.toImmutableList()))));

        ExecutorService executor = new ForkJoinPool();
        try {
            // Compute java lines changed between each commit pair
            List<Integer> diffs =
                    executor.invokeAll(compareThreadTasks).stream()
                            .map((ThrowingFunction<Future<CompareResult>, CompareResult>) Future::get)
                            .sorted(Comparator.comparingInt(result -> result.id))
                            .map(result -> result.diffs)
                            .flatMap(Collection::stream)
                            .collect(Collectors.toList());

            List<RevCommit> result = new ArrayList<>();
            result.add(commits.get(0));

            // Walk through the the diffs taking a commit every threshold amount
            int totalLinesChanged = 0;
            for (int i = 0; i < diffs.size(); ++i) {
                totalLinesChanged += diffs.get(i);

                if (totalLinesChanged >= javaLinesChangedThreshold) {
                    result.add(commits.get(i + 1));
                    totalLinesChanged = 0;
                }
            }

            if (!Iterables.getLast(result).equals(Iterables.getLast(commits))) {
                result.add(Iterables.getLast(commits));
            }

            return result;
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            executor.shutdown();
        }
    }

    private static class CompareTask {
        private final Git git;
        private final RevCommit oldCommit;
        private final RevCommit newCommit;

        CompareTask(final Git git, final RevCommit oldCommit, final RevCommit newCommit) {
            this.git = git;
            this.oldCommit = oldCommit;
            this.newCommit = newCommit;
        }

        public int computeDiff() throws IOException, GitAPIException {
            return computeJavaLinesChanged(git, oldCommit, newCommit);
        }
    }

    private static class CompareResult {
        public final int id;
        public final ImmutableList<Integer> diffs;

        CompareResult(final int id, final ImmutableList<Integer> diffs) {
            this.id = id;
            this.diffs = diffs;
        }
    }
}
