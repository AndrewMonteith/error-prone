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

import com.google.errorprone.bugtrack.DatasetDiagnostic;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public final class FilesDiagnostics {
  private final Map<String, Collection<DatasetDiagnostic>> filesAndDiagnostics;

  public FilesDiagnostics(Collection<DatasetDiagnostic> diagnostics) {
    this.filesAndDiagnostics = new HashMap<>();

    for (DatasetDiagnostic diag : diagnostics) {
      filesAndDiagnostics.computeIfAbsent(diag.getFileName(), file -> new ArrayList<>()).add(diag);
    }
  }

  public Collection<DatasetDiagnostic> getDiagnosticsInFile(String file) {
    return filesAndDiagnostics.get(file);
  }
}
