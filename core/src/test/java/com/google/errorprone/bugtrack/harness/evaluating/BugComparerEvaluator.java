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

import com.google.common.base.Optional;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import com.google.errorprone.bugtrack.BugComparer;
import com.google.errorprone.bugtrack.DatasetDiagnostic;
import com.google.errorprone.bugtrack.PathsComparer;
import com.google.errorprone.bugtrack.SrcFilePairLoader;
import com.google.errorprone.bugtrack.harness.matching.DiagnosticsMatcher;
import com.google.errorprone.bugtrack.harness.matching.MatchResults;
import com.google.errorprone.bugtrack.projects.CorpusProject;
import com.google.errorprone.bugtrack.utils.IOThrowingFunction;
import org.eclipse.jgit.api.errors.GitAPIException;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class BugComparerEvaluator {
    private final CorpusProject project;
    private final DiagnosticsFilePairLoader pairLoader;
    private final MissedLikelihoodCalculator missedLikelihoodCalculator;
    private final int trials;

    public BugComparerEvaluator(CorpusProject project,
                                DiagnosticsFilePairLoader pairLoader,
                                MissedLikelihoodCalculator missedLikelihoodCalculator,
                                final int trials) {
        this.project = project;
        this.pairLoader = pairLoader;
        this.missedLikelihoodCalculator = missedLikelihoodCalculator;
        this.trials = trials;
    }

    private boolean resultsAreDifferent(MatchResults results1, MatchResults results2) {
        if (results1.getUnmatchedOldDiagnostics() != results2.getUnmatchedOldDiagnostics()) {
            return true;
        }

        return Sets.symmetricDifference(
                results1.getMatchedDiagnostics().entrySet(),
                results2.getMatchedDiagnostics().entrySet()).isEmpty();
    }

    private void writeBugComparerDisagreements(FileWriter writer,
                                               MatchResults results1,
                                               MatchResults results2) throws IOException {
        Set<Map.Entry<DatasetDiagnostic, DatasetDiagnostic>> matched1 = results1.getMatchedDiagnostics().entrySet();
        Set<Map.Entry<DatasetDiagnostic, DatasetDiagnostic>> matched2 = results2.getMatchedDiagnostics().entrySet();

        Set<Map.Entry<DatasetDiagnostic, DatasetDiagnostic>> disagreement = Sets.symmetricDifference(matched1, matched2);
        Set<Map.Entry<DatasetDiagnostic, DatasetDiagnostic>> processed = new HashSet<>();

        for (Map.Entry<DatasetDiagnostic, DatasetDiagnostic> entry : disagreement) {
            if (processed.contains(entry)) {
                continue;
            }
            processed.add(entry);

            int entryComparerId = matched1.contains(entry) ? 1 : 2;
            int otherComparerId = entryComparerId == 1 ? 2 : 1;

            Optional<Map.Entry<DatasetDiagnostic, DatasetDiagnostic>> otherTracking = Iterables.tryFind(
                    matched1.contains(entry) ? matched2 : matched1,
                    entry2 -> entry2.getKey().equals(entry.getKey()));

            if (otherTracking.isPresent()) {
                // The other comparer did track the same diagnostic, just disagrees where it ends up
                processed.add(otherTracking.get());
                writer.write("  Bug Comparer Disagreement on diagnostic:\n");
                writer.write(entry.getKey().toString());
                writer.write("  Comparer " + entryComparerId + " says:\n");
                writer.write(entry.getValue().toString());
                writer.write("  Comparer " +  otherComparerId + " says:\n");
                writer.write(otherTracking.get().getValue().toString());
            } else {
                // The other comparer could not track it
                writer.write("  Comparer " + entryComparerId + " tracked the below diagnostic whereas " + otherComparerId + " didn't\n");
                writer.write(entry.getKey().toString());
                writer.write("  to:\n");
                writer.write(entry.getValue().toString());
            }
            writer.write("\n");
        }
    }

    private void writePossiblyMissedDiagnostic(FileWriter writer,
                                               DatasetDiagnostic missedOldDiagnostic,
                                               DatasetDiagnostic mostLikelyNewDiagnostic,
                                               double likelihood) throws IOException {
        writer.write("Neither comparer could track:\n");
        writer.write(missedOldDiagnostic.toString());
        writer.write("but it could match the following with " + likelihood + " confidence\n");
        writer.write(mostLikelyNewDiagnostic.toString());
    }

    private void writeLikelyDiagnosticsNeitherTracked(FileWriter writer,
                                                      SrcFilePairLoader srcFilePairLoader,
                                                      PathsComparer pathsComparer,
                                                      MatchResults results1,
                                                      MatchResults results2) throws IOException {
        // Old diagnostics that weren't tracked by any comparer
        Set<DatasetDiagnostic> untrackedOldDiagnostics = Sets.difference(
                results1.getOldDiagnostics(), // same as results2 old diagnostics
                Sets.union(results1.getMatchedDiagnostics().keySet(), results2.getMatchedDiagnostics().keySet()));

        // New diagnostics now tracked by any comparer
        Set<DatasetDiagnostic> untrackedNewDiagnostics = Sets.difference(
                results1.getNewDiagnostics(),
                Sets.newHashSet(Iterables.concat(
                        results1.getMatchedDiagnostics().values(),
                        results2.getMatchedDiagnostics().values())));

        for (DatasetDiagnostic missedOldDiag : untrackedOldDiagnostics) {
            double bestFoundLikelihood = -1;
            DatasetDiagnostic mostLikelyMatch = null;

            for (DatasetDiagnostic missedNewDiag : untrackedNewDiagnostics) {
                if (!pathsComparer.inSameFile(missedOldDiag, missedNewDiag) || !missedOldDiag.isSameType(missedNewDiag)) {
                    continue;
                }

                double likelihood = missedLikelihoodCalculator.compute(
                        srcFilePairLoader.load(missedOldDiag, missedNewDiag),
                        missedOldDiag,
                        missedNewDiag);

                if (0.3 <= likelihood && bestFoundLikelihood <= likelihood) {
                    mostLikelyMatch = missedNewDiag;
                    bestFoundLikelihood = likelihood;
                }
            }

            if (mostLikelyMatch != null) {
                writePossiblyMissedDiagnostic(writer, missedOldDiag, mostLikelyMatch, bestFoundLikelihood);
            }
        }
    }

    private void writeResults(Path outputDir,
                              DiagnosticsFilePairLoader.Pair oldAndNewDiagFile,
                              SrcFilePairLoader srcFilePairLoader,
                              PathsComparer pathsComparer,
                              MatchResults results1,
                              MatchResults results2) throws IOException {
        Path output = outputDir.resolve(oldAndNewDiagFile.oldFile.name + ".." + oldAndNewDiagFile.newFile.name);
        if (output.toFile().exists()) {
            Files.delete(output);
        }

        try (FileWriter writer = new FileWriter(output.toFile())) {
            writeBugComparerDisagreements(writer, results1, results2);
            writeLikelyDiagnosticsNeitherTracked(
                    writer,
                    srcFilePairLoader,
                    pathsComparer,
                    results1,
                    results2);
        }

    }


    public void compareBugComparers(BugComparerEvaluationConfig config,
                                    Path outputDir) throws Exception {
        if (!outputDir.toFile().exists()) {
            throw new RuntimeException(outputDir + " does not exist");
        }

        Set<DiagnosticsFilePairLoader.Pair> mismatchedPairs = new HashSet<>();

        for (int trial = 0; trial < trials; ++trial) {
            DiagnosticsFilePairLoader.Pair oldAndNewDiagFiles = pairLoader.load();
            System.out.println("Comparing " + oldAndNewDiagFiles.oldFile.commitId + " to " + oldAndNewDiagFiles.newFile.commitId);

            if (mismatchedPairs.contains(oldAndNewDiagFiles))  {
                continue;
            }
            mismatchedPairs.add(oldAndNewDiagFiles);

            MatchResults comparer1Results = DiagnosticsMatcher.fromFiles(
                    project,
                    oldAndNewDiagFiles.oldFile,
                    oldAndNewDiagFiles.newFile,
                    config.createBugComparer1(oldAndNewDiagFiles)).getResults();

            MatchResults comparer2Results = DiagnosticsMatcher.fromFiles(
                    project,
                    oldAndNewDiagFiles.oldFile,
                    oldAndNewDiagFiles.newFile,
                    config.createBugComparer2(oldAndNewDiagFiles)).getResults();

            if (resultsAreDifferent(comparer1Results, comparer2Results)) {
                writeResults(
                        outputDir,
                        oldAndNewDiagFiles,
                        config.createSrcFilePairLoader(oldAndNewDiagFiles),
                        config.createPathComparer(oldAndNewDiagFiles),
                        comparer1Results,
                        comparer2Results);
            }

            System.gc();
        }
    }
}
