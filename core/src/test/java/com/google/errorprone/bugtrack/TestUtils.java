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

package com.google.errorprone.bugtrack;

import com.google.common.collect.ImmutableList;
import com.google.errorprone.bugtrack.harness.scanning.DiagnosticsCollector;
import com.google.errorprone.bugtrack.harness.scanning.DiagnosticsScan;
import com.google.errorprone.bugtrack.motion.DiagnosticsDeltaManager;
import com.google.errorprone.bugtrack.motion.SrcFile;
import com.google.errorprone.bugtrack.motion.SrcFilePair;
import com.google.errorprone.bugtrack.projects.CorpusProject;
import com.google.errorprone.bugtrack.projects.ProjectFile;
import com.google.errorprone.bugtrack.utils.ProjectFiles;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;

public final class TestUtils {
    private static final Path TEST_RESOURCES = Paths.get("src/test/java/com/google/errorprone/bugtrack/testdata/");

    public static List<String> readTestFile(String file) throws IOException {
        Path path = Paths.get(file);
        if (path.toFile().exists()) {
            return Files.readAllLines(path);
        } else {
            return Files.readAllLines(TEST_RESOURCES.resolve(path));
        }
    }

    public static SrcFile readTestSrcFile(String file) throws IOException {
        return new SrcFile(file, readTestFile(file));
    }

    public static SrcFilePair readTestSrcFilePair(String oldFile, String newFile) throws IOException {
        return new SrcFilePair(readTestSrcFile(oldFile), readTestSrcFile(newFile));
    }

    public static DiagnosticsPair compareDiagnostics(String oldFile, String newFile) {
        DiagnosticsScan oldScan = createTestScan("test_old", oldFile);
        DiagnosticsScan newScan = createTestScan("test_new", newFile);

        Collection<DatasetDiagnostic> oldDiagnostics = DiagnosticsCollector.collectDatasetDiagnosics(oldScan);
        Collection<DatasetDiagnostic> newDiagnostics = DiagnosticsCollector.collectDatasetDiagnosics(newScan);

        return new DiagnosticsPair(oldDiagnostics, newDiagnostics);
    }

    private static DiagnosticsScan createTestScan(String name, String file) {
        return new DiagnosticsScan(name, ImmutableList.of(new ProjectFile(TEST_PROJECT, Paths.get(file))), ImmutableList.of());
    }

    public static class DiagnosticsPair {
        public final Collection<DatasetDiagnostic> oldDiagnostics;
        public final Collection<DatasetDiagnostic> newDiagnostics;

        public DiagnosticsPair(final Collection<DatasetDiagnostic> oldDiagnostics,
                               final Collection<DatasetDiagnostic> newDiagnostics) {
            this.oldDiagnostics = oldDiagnostics;
            this.newDiagnostics = newDiagnostics;
        }
    }

    private static CorpusProject TEST_PROJECT = new CorpusProject() {
        @Override
        public Path getRoot() {
            return ProjectFiles.get("error-prone/core/src/test/java/com/google/errorprone/bugtrack/testdata");
        }

        @Override
        public boolean shouldScanFile(Path file) {  return true; }

        @Override
        public BuildSystem getBuildSystem() {
            return BuildSystem.Other;
        }
    };
}