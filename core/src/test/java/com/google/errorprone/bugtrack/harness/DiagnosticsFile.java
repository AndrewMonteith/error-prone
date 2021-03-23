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
import com.google.errorprone.bugtrack.projects.CorpusProject;
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

public class DiagnosticsFile {
    public final String name;
    public final String commitId;
    public final ImmutableCollection<DatasetDiagnostic> diagnostics;

    private DiagnosticsFile(String name, String commitId, ImmutableCollection<DatasetDiagnostic> diagnostics) {
        this.name = name;
        this.commitId = commitId;
        this.diagnostics = diagnostics;
    }

    public static int getSequenceNumberFromName(String fileName) {
        return Integer.parseInt(fileName.split(" ")[0]);
    }

    public static DiagnosticsFile load(CorpusProject project, Path file) throws IOException {
        return load(project, file, diagnostic -> true);
    }

    public static DiagnosticsFile load(CorpusProject project, String file) throws IOException {
        return load(project, Paths.get(file));
    }

    private static String giveFileNameCorrectRoot(CorpusProject project, String fileName) {
        // any project file will be <ROOT>/java-corpus/<PROJECT>/...
        // we want to append ... onto project root
        Path file = Paths.get(fileName);
        int corpusName = Iterables.indexOf(file, path -> path.equals(Paths.get("java-corpus")));
        Path filePath = file.subpath(corpusName + 2, file.getNameCount());

        return project.getRoot().resolve(filePath).toString();
    }

    private static DatasetDiagnostic loadDiagnostic(CorpusProject project, String[] locationDetails, String message) {
        long line = Long.parseLong(locationDetails[1]);
        long col = Long.parseLong(locationDetails[2]);
        String fileName = giveFileNameCorrectRoot(project, locationDetails[0]);

        if (locationDetails.length == 6) { // old version without position
            return new DatasetDiagnostic(fileName, line, col,
                    Long.parseLong(locationDetails[3]),
                    Long.parseLong(locationDetails[4]),
                    Long.parseLong(locationDetails[5]),
                    message);
        } else { // new version with position
            return new DatasetDiagnostic(fileName, line, col,
                    Long.parseLong(locationDetails[3]),
                    -1,
                    Long.parseLong(locationDetails[4]),
                    message);
        }
    }

    public static DiagnosticsFile load(CorpusProject project, Path file, Predicate<DatasetDiagnostic> acceptDiagnostic) throws IOException {
        List<String> lines = Files.readAllLines(file);

        String commitId = lines.get(0).split(" ")[0];
        int expectedNumber = Integer.parseInt(lines.get(0).split(" ")[1]);

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

            DatasetDiagnostic diagnostic = loadDiagnostic(project, locationDetails, message);
            if (!acceptDiagnostic.test(diagnostic) || diagnostics.contains(diagnostic)) {
                continue;
            }

            diagnostics.add(diagnostic);
        }

        if (diagnostics.size() != expectedNumber) {
            throw new RuntimeException("failed to parse expected number for " + file.toString());
        }

        return new DiagnosticsFile(file.toFile().getName(), commitId, ImmutableList.copyOf(diagnostics));
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

    public int getSeqNum() {
        return getSequenceNumberFromName(name);
    }
}
