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

package com.google.errorprone.bugtrack.harness.matching;

import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import com.google.errorprone.bugtrack.BugComparer;
import com.google.errorprone.bugtrack.DatasetDiagnostic;
import com.google.errorprone.bugtrack.GitPathComparer;
import com.google.errorprone.bugtrack.PathsComparer;
import com.google.errorprone.bugtrack.harness.DiagnosticsFile;
import com.google.errorprone.bugtrack.projects.CorpusProject;
import org.eclipse.jgit.api.errors.GitAPIException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public final class DiagnosticsMatcher {
  private static final boolean printMultiMatches = true;
  private final Collection<DatasetDiagnostic> oldDiagnostics;
  private final Collection<DatasetDiagnostic> newDiagnostics;
  private final BugComparer comparer;
  private final PathsComparer pathsComparer;

  public DiagnosticsMatcher(
      Collection<DatasetDiagnostic> oldDiagnostics,
      Collection<DatasetDiagnostic> newDiagnostics,
      BugComparer comparer,
      PathsComparer pathsComparer) {
    this.oldDiagnostics = oldDiagnostics;
    this.newDiagnostics = newDiagnostics;
    this.comparer = comparer;
    this.pathsComparer = pathsComparer;
  }

  public static DiagnosticsMatcher fromFiles(
      CorpusProject project,
      DiagnosticsFile oldDiagFile,
      DiagnosticsFile newDiagFile,
      BugComparer bugComparer)
      throws IOException, GitAPIException {
    return new DiagnosticsMatcher(
        oldDiagFile.diagnostics,
        newDiagFile.diagnostics,
        bugComparer,
        new GitPathComparer(project.loadRepo(), oldDiagFile.commitId, newDiagFile.commitId));
  }

  public MatchResults getResults() {
    Map<DatasetDiagnostic, DatasetDiagnostic> matchedDiagnostics = new HashMap<>();

    Set<String> oldFiles =
        Sets.newHashSet(Iterables.transform(oldDiagnostics, DatasetDiagnostic::getFileName));

    oldFiles.forEach(
        oldFile -> {
          oldDiagnostics.forEach(
              oldDiag -> {
                if (!oldDiag.getFileName().equals(oldFile)
                    || matchedDiagnostics.containsKey(oldDiag)) {
                  return;
                }

                Collection<DatasetDiagnostic> matching =
                    newDiagnostics.stream()
                        .filter(newDiag -> pathsComparer.isSameFile(oldDiag, newDiag))
                        .filter(
                            newDiag ->
                                !matchedDiagnostics.containsValue(newDiag)
                                    && comparer.areSame(oldDiag, newDiag))
                        .collect(Collectors.toList());

                if (matching.size() == 1) {
                  matchedDiagnostics.put(oldDiag, Iterables.getOnlyElement(matching));
                } else if (matching.size() > 1) {
                  if (printMultiMatches) {
                    System.out.println("A diagnostic matched with multiple diagnostics");
                    System.out.println("Old diagnostic:");
                    System.out.println(oldDiag);
                    System.out.println("Candidate new:");
                    matching.forEach(System.out::println);
                  }
                }
              });
        });

    return new MatchResults(oldDiagnostics, newDiagnostics, matchedDiagnostics);
  }

  public void writeToStdout() {
    System.out.println(getResults());
  }

  private void writeLogFile(Path file, Consumer<StringBuilder> buildString) throws IOException {
    StringBuilder sb = new StringBuilder();
    buildString.accept(sb);
    Files.write(file, sb.toString().getBytes(StandardCharsets.UTF_8));
  }

  public void writeToFile(Path outputDir) throws IOException {
    if (!outputDir.toFile().isDirectory()) {
      throw new RuntimeException("can only dump matches into directories");
    }

    final MatchResults results = getResults();

    writeLogFile(
        outputDir.resolve("unmatched_old"),
        fileContents -> results.getUnmatchedOldDiagnostics().forEach(fileContents::append));

    writeLogFile(
        outputDir.resolve("unmatched_new"),
        fileContents -> results.getUnmatchedNewDiagnostics().forEach(fileContents::append));

    final Map<DatasetDiagnostic, DatasetDiagnostic> matchedDiagnostics =
        results.getMatchedDiagnostics();
    writeLogFile(
        outputDir.resolve("matched_old"),
        fileContents -> matchedDiagnostics.keySet().forEach(fileContents::append));

    writeLogFile(
        outputDir.resolve("matched_new"),
        fileContents ->
            matchedDiagnostics
                .keySet()
                .forEach(key -> fileContents.append(matchedDiagnostics.get(key))));
  }
}
