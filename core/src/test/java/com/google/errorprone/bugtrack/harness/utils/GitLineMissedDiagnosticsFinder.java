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

import com.google.errorprone.bugtrack.DatasetDiagnostic;
import com.google.errorprone.bugtrack.utils.GitUtils;
import com.google.errorprone.bugtrack.motion.DiagnosticPositionMotionComparer;
import com.google.errorprone.bugtrack.harness.matching.DiagnosticsMatcher;
import com.google.errorprone.bugtrack.harness.matching.MatchResults;
import com.google.errorprone.bugtrack.motion.GitDiagnosticDeltaManager;
import com.google.errorprone.bugtrack.projects.CorpusProject;
import com.google.errorprone.bugtrack.projects.GuiceProject;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.Test;
import org.openjdk.tools.javac.util.Pair;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;

import static com.google.errorprone.bugtrack.motion.trackers.DPTrackerConstructorFactory.newCharacterLineTracker;

public final class GitLineMissedDiagnosticsFinder {
    private static double computeSimilarityFromString(String oldLine, String newLine) {
        // How much of newLine is a substring of oldLine
        int maxLen = 0;
        for (int i = 0; i < newLine.length(); ++i) {
            for (int j = i; j < newLine.length(); ++j) {
                if (oldLine.contains(newLine.substring(i, j))) {
                    maxLen = Math.max(maxLen, j - i + 1);
                }
            }
        }

        return (double) maxLen / newLine.length();
    }

    private static Map<DatasetDiagnostic, String> loadDiagnosticLines(CorpusProject project, RevCommit commit, Iterable<DatasetDiagnostic> diagnostics) throws IOException {
        Map<DatasetDiagnostic, String> result = new HashMap<>();
        for (DatasetDiagnostic diagnostic : diagnostics) {
            result.put(diagnostic, GitUtils.loadJavaLine(project.loadRepo(), commit, diagnostic));
        }
        return result;
    }

    private static void findLinkelyMissedDiagnostics(MatchResults results,
                                                     BiFunction<DatasetDiagnostic, DatasetDiagnostic, Double> computeSimilarity) {
        for (DatasetDiagnostic unmatchedOldDiag : results.getUnmatchedOldDiagnostics()) {
            System.out.println(unmatchedOldDiag);

            results.getUnmatchedNewDiagnostics().stream()
                    .filter(unmatchedNewDiag -> unmatchedOldDiag.isSameType(unmatchedNewDiag) && unmatchedOldDiag.getFileName().equals(unmatchedNewDiag.getFileName()))
                    .map(unmatchedNewDiag -> new Pair<>(unmatchedNewDiag, computeSimilarity.apply(unmatchedOldDiag, unmatchedNewDiag)))
                    .sorted((p1, p2) -> p2.snd.compareTo(p1.snd)) // sort in descending order
                    .limit(2)
                    .forEach(match -> {
                        System.out.printf("Possibly matching %s %d %d with score %.3f\n",
                                match.fst.getFileName(), match.fst.getLineNumber(), match.fst.getColumnNumber(), match.snd);
                    });

            System.out.print("-----\n\n");
        }
    }

    /*
        Look for new diagnostics of the same type who's diagnostic contains a fragment of or looks similar to the old diagnostic.
     */
    public static void proposeMissedMatchesWithSubstringSimilarity(CorpusProject project,
                                                                   RevCommit oldCommit,
                                                                   RevCommit newCommit,
                                                                   MatchResults results) throws IOException {
        Map<DatasetDiagnostic, String> unmatchedOldDiagLines = loadDiagnosticLines(project, oldCommit, results.getUnmatchedOldDiagnostics());
        Map<DatasetDiagnostic, String> unmatchedNewDiagLines = loadDiagnosticLines(project, newCommit, results.getUnmatchedNewDiagnostics());

        BiFunction<DatasetDiagnostic, DatasetDiagnostic, Double> computeSimilary = (oldDiag, newDiag) -> computeSimilarityFromString(
                unmatchedOldDiagLines.get(oldDiag), unmatchedNewDiagLines.get(newDiag));

        findLinkelyMissedDiagnostics(results, computeSimilary);
    }

    public static void proposeMissedMatchesWithLineDistanceSimilarity(MatchResults results) throws IOException {
        findLinkelyMissedDiagnostics(results,
                (oldDiag, newDiag) -> (double) 3 / (Math.abs(oldDiag.getLineNumber() - newDiag.getLineNumber()) + 1));
    }

    @Test
    public void example_FindingCandidates() throws IOException, GitAPIException {
        // GIVEN:
        CorpusProject project = new GuiceProject();
        RevCommit oldCommit = GitUtils.parseCommit(project.loadRepo(), "875868e7263491291d4f8bdc1332bfea746ad673");
        RevCommit newCommit = GitUtils.parseCommit(project.loadRepo(), "9b371d3663db9db230417f3cc394e72b705d7d7f");

        MatchResults results = DiagnosticsMatcher.fromFiles(
                Paths.get("/home/monty/IdeaProjects/java-corpus/diagnostics/guice/8 875868e7263491291d4f8bdc1332bfea746ad673"),
                Paths.get("/home/monty/IdeaProjects/java-corpus/diagnostics/guice/22 9b371d3663db9db230417f3cc394e72b705d7d7f"),
                new DiagnosticPositionMotionComparer(
                        new GitDiagnosticDeltaManager(project.loadRepo(), oldCommit, newCommit),
                        newCharacterLineTracker())).getResults();

        proposeMissedMatchesWithSubstringSimilarity(project, oldCommit, newCommit, results);
//        proposeMissedMatchesWithLineDistanceSimilarity(results);
    }

    @Test
    public void example_FindingCandidates2() throws IOException, GitAPIException {
        CorpusProject project = new GuiceProject();
        RevCommit oldCommit = GitUtils.parseCommit(project.loadRepo(), "875868e7263491291d4f8bdc1332bfea746ad673");
        RevCommit newCommit = GitUtils.parseCommit(project.loadRepo(), "9b371d3663db9db230417f3cc394e72b705d7d7f");

        MatchResults results = DiagnosticsMatcher.fromFiles(
                Paths.get("/home/monty/IdeaProjects/java-corpus/diagnostics/guice/8 875868e7263491291d4f8bdc1332bfea746ad673"),
                Paths.get("/home/monty/IdeaProjects/java-corpus/diagnostics/guice/22 9b371d3663db9db230417f3cc394e72b705d7d7f"),
                new DiagnosticPositionMotionComparer(
                        new GitDiagnosticDeltaManager(project.loadRepo(), oldCommit, newCommit),
                        newCharacterLineTracker())).getResults();

        proposeMissedMatchesWithSubstringSimilarity(project, oldCommit, newCommit, results);
//        proposeMissedMatchesWithLineDistanceSimilarity(results);
    }

}
