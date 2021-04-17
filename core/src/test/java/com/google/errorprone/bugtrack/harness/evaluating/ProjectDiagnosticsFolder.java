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
import com.google.errorprone.bugtrack.utils.ThrowingFunction;

import java.io.File;
import java.nio.file.Path;
import java.util.Arrays;

public final class ProjectDiagnosticsFolder {

  public static ImmutableList<DiagnosticsFile> load(CorpusProject project, Path diagnosticsFolder) {
    return Arrays.asList(diagnosticsFolder.toFile().listFiles()).stream()
        .sorted(
            (file1, file2) -> {
              Integer id1 = DiagnosticsFile.getSequenceNumberFromName(file1.getName());
              Integer id2 = DiagnosticsFile.getSequenceNumberFromName(file2.getName());
              return id1.compareTo(id2);
            })
        .filter(file -> !Files.getFileExtension(file.toString()).equals("skip"))
        .map(
            (ThrowingFunction<File, DiagnosticsFile>)
                file -> DiagnosticsFile.load(project, file.toPath()))
        .collect(ImmutableList.toImmutableList());
  }
}
