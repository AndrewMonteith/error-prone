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
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.io.Files;
import com.google.errorprone.bugtrack.harness.utils.CommitPair;
import com.google.errorprone.bugtrack.util.ThrowingFunction;
import com.google.errorprone.bugtrack.utils.GitUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.patch.FileHeader;
import org.eclipse.jgit.revwalk.RevCommit;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import static com.google.errorprone.bugtrack.harness.utils.ListUtils.consecutivePairs;

public class JavaLinesChangedFilter implements CommitPathFilter {
  private final int javaLinesChangedThreshold;
  private final Git git;

  public JavaLinesChangedFilter(Git git, int javaLinesChangedThreshold) {
    this.javaLinesChangedThreshold = javaLinesChangedThreshold;
    this.git = git;
  }

  private int computeJavaLinesChanged(CommitPair commitPair) throws IOException, GitAPIException {
    List<DiffEntry> diffs = GitUtils.computeDiffs(git, commitPair.oldCommit, commitPair.newCommit);

    DiffFormatter formatter = new DiffFormatter(null);
    formatter.setRepository(git.getRepository());
    formatter.setDetectRenames(true);

    int totalLinesChanged = 0;
    for (DiffEntry diff : diffs) {
      if (!Files.getFileExtension(diff.getNewPath()).equals("java")) {
        continue;
      }

      FileHeader header = formatter.toFileHeader(diff);

      totalLinesChanged +=
          header.toEditList().stream()
              .mapToInt(edit -> edit.getLengthA() + edit.getLengthB())
              .sum();
    }

    return totalLinesChanged;
  }

  private int computeTasksPerThread(final int compareTasks) {
    final int processors = Runtime.getRuntime().availableProcessors();

    if (compareTasks / processors == 0) {
      return 4;
    } else {
      return Math.max(compareTasks / processors / 4, 1);
    }
  }

  private List<CommitPair> computeAllPairs(List<RevCommit> commitPath) {
    Set<CommitPair> pairs = new HashSet<>();
    consecutivePairs(
        commitPath, (oldCommit, newCommit) -> pairs.add(CommitPair.of(oldCommit, newCommit)));
    return ImmutableList.copyOf(pairs);
  }

  private List<Callable<Map<CommitPair, Integer>>> allocatePairsToCores(List<CommitPair> pairs) {
    List<Callable<Map<CommitPair, Integer>>> tasks = new ArrayList<>();

    Lists.partition(pairs, computeTasksPerThread(pairs.size()))
        .forEach(
            pairsSubset ->
                tasks.add(
                    () ->
                        pairsSubset.stream()
                            .collect(
                                Collectors.toMap(
                                    commitPath -> commitPath,
                                    (ThrowingFunction<CommitPair, Integer>)
                                        this::computeJavaLinesChanged))));

    return ImmutableList.copyOf(tasks);
  }

  private Map<CommitPair, Integer> computeCommitDiffs(
      List<Callable<Map<CommitPair, Integer>>> coreWorkloads) {
    ExecutorService executor = new ForkJoinPool();
    try {
      return executor.invokeAll(coreWorkloads).stream()
          .map(
              (ThrowingFunction<Future<Map<CommitPair, Integer>>, Map<CommitPair, Integer>>)
                  Future::get)
          .reduce(
              (diffMap1, diffMap2) ->
                  ImmutableMap.<CommitPair, Integer>builder()
                      .putAll(diffMap1)
                      .putAll(diffMap2)
                      .build())
          .get();
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    } finally {
      executor.shutdown();
    }
  }

  private List<RevCommit> filterPath(List<RevCommit> path, Map<CommitPair, Integer> diffs) {
    List<RevCommit> newPath = new ArrayList<>();

    newPath.add(path.get(0));

    int totalLinesChanged = 0;
    for (int i = 0; i < path.size() - 1; ++i) {
      CommitPair commitPair = CommitPair.of(path.get(i), path.get(i + 1));
      totalLinesChanged += diffs.get(commitPair);

      if (totalLinesChanged >= javaLinesChangedThreshold) {
        newPath.add(commitPair.newCommit);
        totalLinesChanged = 0;
      }
    }

    if (!Iterables.getLast(newPath).equals(Iterables.getLast(path))) {
      newPath.add(Iterables.getLast(path));
    }

    return ImmutableList.copyOf(newPath);
  }

  @Override
  public List<RevCommit> filter(List<RevCommit> commitPath) throws IOException, GitAPIException {
    List<CommitPair> commitPairs = computeAllPairs(commitPath);

    List<Callable<Map<CommitPair, Integer>>> coreWorkloads = allocatePairsToCores(commitPairs);

    Map<CommitPair, Integer> diffMap = computeCommitDiffs(coreWorkloads);

    return filterPath(commitPath, diffMap);
  }
}
