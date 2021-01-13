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

import com.google.common.collect.Iterables;
import com.google.errorprone.CompilationTestHelper;
import com.google.errorprone.bugtrack.projects.ProjectFile;
import com.google.errorprone.scanner.BuiltInCheckerSuppliers;

import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import java.util.*;
import java.util.stream.Collectors;

public final class DiagnosticsCollector {
    public static Collection<Diagnostic<? extends JavaFileObject>> collectDiagnostics(DiagnosticsScan scan) {
        if (scan.files.isEmpty()) {
            return Collections.emptyList();
        }

        CompilationTestHelper helper = CompilationTestHelper.newInstance(BuiltInCheckerSuppliers.defaultChecks(), DiagnosticsCollector.class);

        Collection<ProjectFile> files = scan.files.stream()
                .filter(ProjectFile::exists)
                .collect(Collectors.toList());

        if (files.isEmpty()) {
            return Collections.emptyList();
        }

        files.forEach(projFile -> helper.addSourceFile(projFile.toFile().toPath()));
        helper.setArgs(scan.cmdLineArguments);

        return helper.collectDiagnostics();
    }

    public static Collection<Diagnostic<? extends JavaFileObject>> collectDiagnostics(Iterable<DiagnosticsScan> scans) {
        List<Diagnostic<? extends JavaFileObject>> diagnostics = new ArrayList<>();
        scans.forEach(scan -> diagnostics.addAll(collectDiagnostics(scan)));
        return diagnostics;
    }

    public static Collection<Diagnostic<? extends JavaFileObject>> collectDiagnostics(Iterable<DiagnosticsScan> scans, boolean printProgress) {
        List<Diagnostic<? extends JavaFileObject>> diagnostics = new ArrayList<>();

        int current = 1, numberOfScans = Iterables.size(scans);
        for (DiagnosticsScan scan : scans) {
            if (printProgress) {
                System.out.printf("Collecting diagnostics for target [%d / %d]\n", current, numberOfScans);
                ++current;
            }

            diagnostics.addAll(collectDiagnostics(scan));
        }

        return diagnostics;
    }

    public static void printDiagnosticsDistribution(List<Diagnostic<? extends JavaFileObject>> diagnostics) {
        final Map<String, Integer> distribution = new HashMap<>();
        diagnostics.forEach(diagnostic -> {
            String msg = diagnostic.getMessage(null);
            String diagnosticType = msg.substring(1, msg.indexOf(']'));

            distribution.put(diagnosticType, distribution.getOrDefault(diagnosticType, 0) + 1);
        });

        distribution.forEach((diagnosticType, frequency) ->
                System.out.printf("%s occured %d times\n", diagnosticType, frequency));
    }
}
