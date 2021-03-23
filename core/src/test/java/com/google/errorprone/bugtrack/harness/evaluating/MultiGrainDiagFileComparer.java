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

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.errorprone.bugtrack.harness.matching.GitCommitMatcher;
import com.google.errorprone.bugtrack.harness.DiagnosticsFile;
import com.google.errorprone.bugtrack.harness.matching.MatchResults;
import com.google.errorprone.bugtrack.projects.CorpusProject;
import org.eclipse.jgit.api.errors.GitAPIException;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

import static com.google.errorprone.bugtrack.motion.trackers.DPTrackerConstructorFactory.*;

public final class MultiGrainDiagFileComparer {
    private final CorpusProject project;
    private final Map<CommitPair, MatchResults> resultsCache;

    private MultiGrainDiagFileComparer(CorpusProject project) {
        this.project = project;
        this.resultsCache = new HashMap<>();
    }

    public static void compareFiles(CorpusProject project,
                                    Path output,
                                    List<GrainDiagFile> grainFiles) throws IOException, GitAPIException {
        new MultiGrainDiagFileComparer(project).compare(output, grainFiles);
    }

    private MatchResults scan(DiagnosticsFile last, DiagnosticsFile next) throws IOException, GitAPIException {
        CommitPair cp = new CommitPair(last.commitId, next.commitId);
        if (resultsCache.containsKey(cp)) {
            System.out.println("retrieved " + cp + " from cache");
            return resultsCache.get(cp);
        }

        MatchResults results = GitCommitMatcher.compareGit(project, last, next)
                .trackIdentical()
                .trackPosition(any(newTokenizedLineTracker(), newIJMStartAndEndTracker()))
                .match();

        resultsCache.put(cp, results);

        return results;
    }

    private void scanWalk(Path output, Iterable<GrainDiagFile> grainFiles) throws IOException, GitAPIException {
        Iterator<GrainDiagFile> iter = grainFiles.iterator();

        DiagnosticsFile last = null;
        while (iter.hasNext()) {
            DiagnosticsFile next = iter.next().getDiagFile();
            if (last == null || last.commitId.equals(next.commitId)) {
                last = next;
                continue;
            }

            String name = last.getSeqNum() + " " + last.commitId + " -> " + next.getSeqNum() + " " + next.commitId;
            System.out.println("Scanning " + name);

            scan(last, next).save(output.resolve(name));

            last = next;
        }
    }

    private void compare(Path output, List<GrainDiagFile> grainFiles) throws IOException, GitAPIException {
        // Get set of all grains
        Set<Integer> grains = grainFiles.stream()
                .map(GrainDiagFile::getGrains)
                .reduce(ImmutableSet.of(), (s1, s2) ->
                        ImmutableSet.<Integer>builder()
                                .addAll(s1)
                                .addAll(s2)
                                .build()
                );

        // Scan each sequence of commits capture at a grain
        for (int grain : grains) {
            System.out.println("scanning grain " + grain);
            scanWalk(output.resolve("grain-" + grain),
                    Iterables.filter(grainFiles, grainFile -> grainFile.hasGrain(0) || grainFile.hasGrain(grain)));
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
