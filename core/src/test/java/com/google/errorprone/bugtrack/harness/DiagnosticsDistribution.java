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

import com.google.errorprone.bugtrack.DatasetDiagnostic;

import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import java.util.HashMap;
import java.util.Map;

import static com.google.errorprone.bugtrack.DiagnosticUtils.extractDiagnosticType;

public final class DiagnosticsDistribution {
    private final Map<String, Integer> diagnosticCounts;

    private DiagnosticsDistribution() {
        diagnosticCounts = new HashMap<>();
    }

    public static DiagnosticsDistribution fromDiagnostics(Iterable<Diagnostic<? extends JavaFileObject>> diagnostics) {
        DiagnosticsDistribution distribution = new DiagnosticsDistribution();
        diagnostics.forEach(distribution::addDiagnostic);
        return distribution;
    }

    public static DiagnosticsDistribution fromDiagnosticKinds(Iterable<String> diagnostics) {
        DiagnosticsDistribution distribution = new DiagnosticsDistribution();
        diagnostics.forEach(distribution::addDiagnostic);
        return distribution;
    }

    public DiagnosticsDistribution(Iterable<DatasetDiagnostic> diagnostics) {
        this();

        diagnostics.forEach(this::addDiagnostic);
    }

    private void addDiagnostic(Diagnostic<? extends JavaFileObject> diagnostic) {
        addDiagnostic(extractDiagnosticType(diagnostic));
    }

    private void addDiagnostic(String diagnosticKind) {
        diagnosticCounts.put(diagnosticKind, diagnosticCounts.getOrDefault(diagnosticKind, 0) + 1);
    }

    private void addDiagnostic(DatasetDiagnostic diagnostic) {
        addDiagnostic(extractDiagnosticType(diagnostic));
    }

    public int getDiagnosticKindCount(String diagnosticKind) {
        return diagnosticCounts.getOrDefault(diagnosticKind, 0);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DiagnosticsDistribution that = (DiagnosticsDistribution) o;

        if (diagnosticCounts.size() != that.diagnosticCounts.size()) {
            return false;
        }

        return diagnosticCounts.keySet().stream()
                .allMatch(diagnosticKind ->
                        getDiagnosticKindCount(diagnosticKind) == that.getDiagnosticKindCount(diagnosticKind));

    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        result.append("Distribution on ").append(diagnosticCounts.values().stream().mapToInt(i -> i).sum()).append(":\n");

        diagnosticCounts.forEach((diagnosticType, frequency) -> {
            result.append(diagnosticType).append(" ").append(frequency).append("\n");
        });

        return result.toString();
    }
}
