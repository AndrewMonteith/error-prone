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

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.io.Files;
import com.google.errorprone.bugtrack.harness.DiagnosticsFile;
import com.google.errorprone.bugtrack.projects.CorpusProject;
import com.google.errorprone.bugtrack.util.ThrowingFunction;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class GrainDiagFile {
    private final ImmutableSet<Integer> grains;
    private final DiagnosticsFile diagFile;

    public DiagnosticsFile getDiagFile() {
        return diagFile;
    }

    public ImmutableSet<Integer> getGrains() {
        return grains;
    }

    public boolean hasGrain(int grain) {
        return grains.contains(grain);
    }

    public GrainDiagFile(CorpusProject project, Path file) throws IOException {
        Iterable<String> stringGrains = Splitter.on("-").split(Files.getFileExtension(file.toString()));
        this.grains = ImmutableSet.copyOf(Iterables.transform(stringGrains, Integer::parseInt));
        this.diagFile = DiagnosticsFile.load(project, file);
    }

    public static List<GrainDiagFile> loadSortedFiles(CorpusProject project, Path files) throws IOException {
        try (Stream<Path> fileStream = java.nio.file.Files.list(files)) {
            return fileStream
                    .filter(file -> !java.nio.file.Files.isDirectory(file))
                    .map(((ThrowingFunction<Path, GrainDiagFile>) file -> new GrainDiagFile(project, file)))
                    .sorted(Comparator.comparingInt(grainFile -> grainFile.diagFile.getSeqNum()))
                    .collect(Collectors.toList());
        }
    }
}
