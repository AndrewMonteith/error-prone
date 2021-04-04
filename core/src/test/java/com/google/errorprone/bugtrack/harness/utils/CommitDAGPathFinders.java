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

public class CommitDAGPathFinders {
  private final RevCommit startCommit;
  private final RevCommit endCommit;
  private final Map<RevCommit, List<RevCommit>> adjacencyList;

  private CommitDAGPathFinders(final Repository repo, final CommitRange range) throws IOException {
    this.startCommit = GitUtils.parseCommit(repo, range.startCommit);
    this.endCommit = GitUtils.parseCommit(repo, range.finalCommit);
    this.adjacencyList = buildAdjacencyList(repo, range);
  }

  public static CommitDAGPathFinders in(final Repository repo, final CommitRange range)
      throws IOException {
    return new CommitDAGPathFinders(repo, range);
  }

  private static Map<RevCommit, List<RevCommit>> buildAdjacencyList(
      Repository repo, CommitRange commitRange) throws IOException {
    Map<RevCommit, List<RevCommit>> adjList = new HashMap<>();
    RevCommit startCommit = GitUtils.parseCommit(repo, commitRange.startCommit);
    RevCommit endCommit = GitUtils.parseCommit(repo, commitRange.finalCommit);

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

  public List<RevCommit> dfs() {
    return new DFSSearch(adjacencyList).find(startCommit, endCommit);
  }

  public List<RevCommit> longest() {
    return new LongestPath(adjacencyList).find(startCommit, endCommit);
  }

  @FunctionalInterface
  private interface PathFinder {
    List<RevCommit> find(RevCommit start, RevCommit goal);
  }

  private static class LongestPath implements PathFinder {
    // https://www.geeksforgeeks.org/find-longest-path-directed-acyclic-graph/

    private final Set<RevCommit> visited;
    private final Stack<RevCommit> topologicalOrdering;
    private final Map<RevCommit, List<RevCommit>> adjacencyList;

    public LongestPath(Map<RevCommit, List<RevCommit>> adjacencyList) {
      this.visited = new HashSet<>();
      this.topologicalOrdering = new Stack<>();
      this.adjacencyList = adjacencyList;
    }

    private static final List<RevCommit> EMPTY_LIST = ImmutableList.of();

    private void topologicalVisit(RevCommit commit) {
      visited.add(commit);

      for (RevCommit adj : adjacencyList.getOrDefault(commit, EMPTY_LIST)) {
        if (!visited.contains(adj)) {
          topologicalVisit(adj);
        }
      }

      topologicalOrdering.push(commit);
    }

    private Map<RevCommit, RevCommit> visitNodesInTopologicalOrdering() {
      Map<RevCommit, Integer> distance = new HashMap<>();
      Map<RevCommit, RevCommit> predecessor = new HashMap<>();

      for (RevCommit commit : topologicalOrdering) {
        final int longestPathToCommit = distance.getOrDefault(commit, 0);

        for (RevCommit neighbour : adjacencyList.getOrDefault(commit, EMPTY_LIST)) {
          final int knownLongestPath = distance.getOrDefault(neighbour, 0);
          if (knownLongestPath < 1 + longestPathToCommit) {
            predecessor.put(neighbour, commit);
            distance.put(neighbour, 1 + knownLongestPath);
          }
        }
      }

      return predecessor;
    }

    @Override
    public List<RevCommit> find(RevCommit start, RevCommit goal) {
      topologicalVisit(start);

      Map<RevCommit, RevCommit> predecessorMap = visitNodesInTopologicalOrdering();
      List<RevCommit> longestPath = new ArrayList<>();

      RevCommit current = predecessorMap.get(goal);
      while (!current.equals(start)) {
        longestPath.add(current);
        current = predecessorMap.get(current);
      }

      return longestPath;
    }
  }

  private static class DFSSearch implements PathFinder {
    private final Map<RevCommit, List<RevCommit>> adjList;

    public DFSSearch(Map<RevCommit, List<RevCommit>> adjList) {
      this.adjList = adjList;
    }

    @Override
    public List<RevCommit> find(final RevCommit start, final RevCommit goal) {
      RevCommit current = start;

      Stack<RevCommit> path = new Stack<>();
      Set<RevCommit> visited = new HashSet<>();

      visit_again:
      while (!current.equals(goal)) {
        for (RevCommit adjacent : adjList.get(current)) {
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

      path.add(0, start);

      return ImmutableList.copyOf(path);
    }
  }
}
