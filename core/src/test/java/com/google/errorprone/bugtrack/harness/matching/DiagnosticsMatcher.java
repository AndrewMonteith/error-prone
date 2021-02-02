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
import com.google.errorprone.bugtrack.BugComparer;
import com.google.errorprone.bugtrack.DatasetDiagnostic;
import com.google.errorprone.bugtrack.DatasetDiagnosticsFile;
import com.google.errorprone.bugtrack.harness.Verbosity;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public final class DiagnosticsMatcher {
    private final Collection<DatasetDiagnostic> oldDiagnostics;
    private final Collection<DatasetDiagnostic> newDiagnostics;
    private final BugComparer comparer;
    private final Verbosity verbose;

    public DiagnosticsMatcher(Collection<DatasetDiagnostic> oldDiagnostics,
                              Collection<DatasetDiagnostic> newDiagnostics,
                              BugComparer comparer) {
        this(oldDiagnostics, newDiagnostics, comparer, Verbosity.SILENT);
    }

    public DiagnosticsMatcher(Collection<DatasetDiagnostic> oldDiagnostics,
                              Collection<DatasetDiagnostic> newDiagnostics,
                              BugComparer comparer,
                              Verbosity verbose) {
        this.oldDiagnostics = oldDiagnostics;
        this.newDiagnostics = newDiagnostics;
        this.comparer = comparer;
        this.verbose = verbose;
    }

    public static DiagnosticsMatcher fromFiles(Path oldFile, Path newFile, BugComparer comparer) throws IOException {
        return fromFiles(oldFile, newFile, comparer, Verbosity.SILENT);
    }

    public static DiagnosticsMatcher fromFiles(Path oldFile, Path newFile, BugComparer comparer, Verbosity verbose) throws IOException {
        DatasetDiagnosticsFile oldDiagnositcsFile = DatasetDiagnosticsFile.loadFromFile(oldFile);
        DatasetDiagnosticsFile newDiagnositcsFile = DatasetDiagnosticsFile.loadFromFile(newFile);

        return new DiagnosticsMatcher(oldDiagnositcsFile.diagnostics, newDiagnositcsFile.diagnostics, comparer, verbose);
    }

    public MatchResults getResults() {
        Map<DatasetDiagnostic, DatasetDiagnostic> matchedDiagnostics = new HashMap<>();

        oldDiagnostics.forEach(oldDiagnostic -> {
            Iterable<DatasetDiagnostic> matching = Iterables.filter(newDiagnostics,
                    newDiagnostic -> !matchedDiagnostics.containsValue(newDiagnostic) && comparer.areSame(oldDiagnostic, newDiagnostic));

            if (Iterables.size(matching) == 1) {
                matchedDiagnostics.put(oldDiagnostic, Iterables.getOnlyElement(matching));
            } else if (Iterables.size(matching) > 1) {
                if (verbose == Verbosity.VERBOSE) {
                    System.out.println("A diagnostic matched with multiple diagnostics");
                    System.out.println("Old diagnostic:");
                    System.out.println(oldDiagnostic);
                    System.out.println("Candidate new:");
                    matching.forEach(System.out::println);
                }
            }
        });

        return new MatchResults(oldDiagnostics, newDiagnostics, matchedDiagnostics);
    }

    public void writeToStdout() {
        System.out.println(getResults());
    }
}
