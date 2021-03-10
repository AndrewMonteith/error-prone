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
import com.google.errorprone.bugtrack.harness.DiagnosticsFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public final class LiveDatasetFilePairLoader implements DiagnosticsFilePairLoader {
    private final ImmutableList<File> validFiles;
    private final Random rnd = new Random();

    public LiveDatasetFilePairLoader(Path folderOfDiagnosticFiles, Predicate<File> includeFile) {
        if (!folderOfDiagnosticFiles.toFile().isDirectory()) {
            throw new RuntimeException(folderOfDiagnosticFiles + " is not a directory!");
        }

        List<File> allFiles = Arrays.asList(folderOfDiagnosticFiles.toFile().listFiles());
        allFiles.sort((file1, file2) -> {
            Integer id1 = DiagnosticsFile.getSequenceNumberFromName(file1.getName());
            Integer id2 = DiagnosticsFile.getSequenceNumberFromName(file2.getName());
            return id1.compareTo(id2);
        });


        this.validFiles = ImmutableList.copyOf(allFiles.stream().filter(includeFile).collect(Collectors.toList()));
    }

    public static LiveDatasetFilePairLoader inSeqNumRange(String folderOfDiagnosticFiles, IntRanges validSequenceNums) {
        return inSeqNumRange(Paths.get(folderOfDiagnosticFiles), validSequenceNums);
    }

    public static LiveDatasetFilePairLoader inSeqNumRange(Path folderOfDiagnosticFiles, IntRanges validSequenceNums) {
        return new LiveDatasetFilePairLoader(
                folderOfDiagnosticFiles, diagnosticsFile ->
                validSequenceNums.contains(DiagnosticsFile.getSequenceNumberFromName(diagnosticsFile.getName())));
    }

    public static LiveDatasetFilePairLoader allFiles(String folderOfDiagnosticsFiles) {
        return new LiveDatasetFilePairLoader(Paths.get(folderOfDiagnosticsFiles), diagnosticsFile -> true);
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

        DiagnosticsFile oldFile = DiagnosticsFile.load(validFiles.get(i1).toPath());
        DiagnosticsFile newFile = DiagnosticsFile.load(validFiles.get(i2).toPath());

        return new Pair(oldFile, newFile);
    }

    public int getNumberOfFiles() {
        return validFiles.size();
    }
}
