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
import com.google.common.io.Files;
import com.google.errorprone.bugtrack.harness.DiagnosticsFile;
import com.google.errorprone.bugtrack.projects.CorpusProject;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Random;
import java.util.function.Predicate;

public final class RandomDiagFilePairLoader implements DiagnosticsFilePairLoader {
  private final ImmutableList<DiagnosticsFile> validFiles;
  private final Random rnd = new Random();

  public RandomDiagFilePairLoader(
      CorpusProject project, Path folderOfDiagnosticFiles, Predicate<DiagnosticsFile> includeFile) {
    if (!folderOfDiagnosticFiles.toFile().isDirectory()) {
      throw new RuntimeException(folderOfDiagnosticFiles + " is not a directory!");
    }

    this.validFiles =
        ProjectDiagnosticsFolder.load(project, folderOfDiagnosticFiles).stream()
            .filter(includeFile)
            .collect(ImmutableList.toImmutableList());
  }

  public static RandomDiagFilePairLoader inSeqNumRange(
      CorpusProject project, String folderOfDiagnosticFiles, IntRanges validSequenceNums) {
    return inSeqNumRange(project, Paths.get(folderOfDiagnosticFiles), validSequenceNums);
  }

  public static RandomDiagFilePairLoader inSeqNumRange(
      CorpusProject project, Path folderOfDiagnosticFiles, IntRanges validSequenceNums) {
    return new RandomDiagFilePairLoader(
        project,
        folderOfDiagnosticFiles,
        diagnosticsFile -> validSequenceNums.contains(diagnosticsFile.getSeqNum()));
  }

  public static RandomDiagFilePairLoader allFiles(
      CorpusProject project, String folderOfDiagnosticsFiles) {
    return allFiles(project, Paths.get(folderOfDiagnosticsFiles));
  }

  public static RandomDiagFilePairLoader allFiles(
      CorpusProject project, Path folderOfDiagnosticsFiles) {
    return new RandomDiagFilePairLoader(project, folderOfDiagnosticsFiles, diagnosticsFile -> true);
  }

  public static DiagnosticsFilePairLoader specificPairs(
      CorpusProject project, String folderOfDiagnosticsFiles, int... pairs) {
    if (pairs.length % 2 == 1) {
      throw new RuntimeException("mismatched pairs");
    }

    RandomDiagFilePairLoader loader =
        new RandomDiagFilePairLoader(
            project, Paths.get(folderOfDiagnosticsFiles), diagnosticFile -> true);
    final int[] pairNum = {0}; // workaround needed to mutate in a lambda

    return () -> {
      int i1 = pairs[pairNum[0]];
      int i2 = pairs[pairNum[0] + 1];
      pairNum[0] += 2;

      return loader.get(i1, i2);
    };
  }

  public static DiagnosticsFilePairLoader notMarkedAsSkipped(
      CorpusProject project, Path folderOfDiagnosticsFiles) {
    return new RandomDiagFilePairLoader(
        project,
        folderOfDiagnosticsFiles,
        file -> !Files.getFileExtension(file.toString()).equals(".skip"));
  }

  private int getRandomId() {
    return rnd.nextInt(validFiles.size());
  }

  @Override
  public Pair load() throws IOException {
    int i1 = getRandomId(), i2 = getRandomId();
    while (i1 >= i2) {
      i1 = getRandomId();
      i2 = getRandomId();
    }

    return get(i1, i2);
  }

  private Pair get(int i1, int i2) {
    return new Pair(validFiles.get(i1), validFiles.get(i2));
  }

  public int getNumberOfFiles() {
    return validFiles.size();
  }
}
