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

package com.google.errorprone.bugtrack.harness.utils;

import com.google.common.collect.ImmutableList;
import com.google.errorprone.bugtrack.CommitRange;
import com.google.errorprone.bugtrack.utils.GitUtils;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;

import java.io.IOException;
import java.util.*;

public class CommitDAGPathFinder {
  private final RevCommit startCommit;
  private final RevCommit endCommit;
  private final Map<RevCommit, List<RevCommit>> adjacencyList;

  private CommitDAGPathFinder(final Repository repo, final CommitRange range) throws IOException {
    this.startCommit = GitUtils.parseCommit(repo, range.startCommit);
    this.endCommit = GitUtils.parseCommit(repo, range.finalCommit);
    this.adjacencyList = buildAdjacencyList(repo);
  }

  public static List<RevCommit> find(final Repository repo, final CommitRange range)
      throws IOException {
    return ImmutableList.copyOf(new CommitDAGPathFinder(repo, range).find());
  }

  private List<RevCommit> find() throws IOException {
    RevCommit current = startCommit;

    Stack<RevCommit> path = new Stack<>();
    Set<RevCommit> visited = new HashSet<>();

    visit_again:
    while (!current.equals(endCommit)) {
      for (RevCommit adjacent : adjacencyList.get(current)) {
        if (!visited.contains(adjacent)) {
          path.add(adjacent);
          visited.add(adjacent);
          current = adjacent;
          continue visit_again;
        }

        if (path.empty()) {
          throw new RuntimeException("could not find a path");
        }

        current = path.pop();
      }
    }

    path.add(0, startCommit);

    return ImmutableList.copyOf(path);
  }

  private Map<RevCommit, List<RevCommit>> buildAdjacencyList(final Repository repo)
      throws IOException {
    Map<RevCommit, List<RevCommit>> adjList = new HashMap<>();

    try (RevWalk walk = new RevWalk(repo)) {
      walk.markStart(endCommit);
      walk.markUninteresting(startCommit);

      for (RevCommit commit : walk) {
        if (commit.equals(startCommit)) {
          break;
        }
        for (RevCommit parent : commit.getParents()) {
          adjList.computeIfAbsent(parent, __ -> new ArrayList<>()).add(commit);
        }
      }
    }

    return adjList;
  }
}
