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

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.errorprone.bugtrack.DatasetDiagnostic;
import org.eclipse.jgit.revwalk.RevCommit;

import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public class DatasetDiagnosticsFile {
    public final String commitId;
    public final ImmutableCollection<DatasetDiagnostic> diagnostics;

    private DatasetDiagnosticsFile(String commitId, ImmutableCollection<DatasetDiagnostic> diagnostics) {
        this.commitId = commitId;
        this.diagnostics = diagnostics;
    }

    public static DatasetDiagnosticsFile loadFromFile(Path file) throws IOException {
        return loadFromFile(file, diagnostic -> true);
    }

    public static DatasetDiagnosticsFile loadFromFile(Path file, Predicate<DatasetDiagnostic> acceptDiagnostic) throws IOException {
        List<String> lines = Files.readAllLines(file);

        String commitId = lines.get(0).split(" ")[0];

        List<DatasetDiagnostic> diagnostics = new ArrayList<>();
        for (int i = 1; i < lines.size(); ) {
            final int startPos = i + 1;

            do {
                ++i;
            } while (i != lines.size() && !lines.get(i).equals("----DIAGNOSTIC"));

            String[] locationDetails = lines.get(startPos).split(" ");

            String message, signature;
            if (lines.get(i - 1).startsWith("Signature ")) {
                message = Joiner.on('\n').join(lines.subList(startPos + 1, i - 1));
                signature = lines.get(i - 1);
            } else {
                message = Joiner.on('\n').join(lines.subList(startPos + 1, i));
            }

            // TODO: Add signature to DatasetDiagnostic
            DatasetDiagnostic diagnostic = new DatasetDiagnostic(
                    locationDetails[0],
                    Long.parseLong(locationDetails[1]),
                    Long.parseLong(locationDetails[2]),
                    Long.parseLong(locationDetails[3]),
                    Long.parseLong(locationDetails[4]),
                    message);

            if (!acceptDiagnostic.test(diagnostic) || diagnostics.contains(diagnostic)) {
                continue;
            }

            diagnostics.add(diagnostic);
        }

        return new DatasetDiagnosticsFile(commitId, ImmutableList.copyOf(diagnostics));
    }

    public static void save(String output, RevCommit commit,
                            Iterable<Diagnostic<? extends JavaFileObject>> diagnostics) throws IOException {
        save(Paths.get(output), commit, diagnostics);
    }

    public static void save(Path output, RevCommit commit,
                            Iterable<Diagnostic<? extends JavaFileObject>> diagnostics) throws IOException {
        StringBuilder fileOutput = new StringBuilder();
        fileOutput.append(commit.getName()).append(" ").append(Iterables.size(diagnostics)).append("\n");
        diagnostics.forEach(diag -> fileOutput.append(new DatasetDiagnostic(diag)));
        Files.write(output, fileOutput.toString().getBytes());
    }
}
