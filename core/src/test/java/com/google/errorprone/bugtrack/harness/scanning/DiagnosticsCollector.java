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

package com.google.errorprone.bugtrack.harness.scanning;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.errorprone.BugCheckerInfo;
import com.google.errorprone.CompilationTestHelper;
import com.google.errorprone.bugpatterns.UnnecessarilyFullyQualified;
import com.google.errorprone.bugtrack.GitUtils;
import com.google.errorprone.bugtrack.harness.Verbosity;
import com.google.errorprone.bugtrack.projects.CorpusProject;
import com.google.errorprone.bugtrack.projects.ProjectFile;
import com.google.errorprone.scanner.BuiltInCheckerSuppliers;
import com.google.errorprone.scanner.ScannerSupplier;
import org.eclipse.jgit.revwalk.RevCommit;

import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public final class DiagnosticsCollector {
    private static Collection<Diagnostic<? extends JavaFileObject>> collectDiagnostics(DiagnosticsScan scan) {
        if (scan.files.isEmpty()) {
            return Collections.emptyList();
        }

        Iterable<BugCheckerInfo> allChecksButVarChecker = Iterables.filter(Iterables.concat(
                BuiltInCheckerSuppliers.ENABLED_ERRORS,
                BuiltInCheckerSuppliers.ENABLED_WARNINGS,
                BuiltInCheckerSuppliers.DISABLED_CHECKS),
            check -> !check.canonicalName().equals("Var"));

//        CompilationTestHelper helper = CompilationTestHelper.newInstance(ScannerSupplier.fromBugCheckerInfos(allChecksButVarChecker), DiagnosticsCollector.class);
        CompilationTestHelper helper = CompilationTestHelper.newInstance(BuiltInCheckerSuppliers.defaultChecks(), DiagnosticsCollector.class);

        Collection<ProjectFile> files = scan.files.stream()
                .filter(ProjectFile::exists)
                .collect(Collectors.toList());

        if (files.isEmpty()) {
            return Collections.emptyList();
        }

        files.forEach(projFile -> helper.addSourceFile(projFile.toFile().toPath()));
        helper.setArgs(ImmutableList.copyOf(Iterables.concat(scan.cmdLineArguments, ImmutableList.of("-Xjcov"))));

        return helper.collectDiagnostics();
    }

    private static Collection<Diagnostic<? extends JavaFileObject>> collectDiagnostics(Iterable<DiagnosticsScan> scans) {
        List<Diagnostic<? extends JavaFileObject>> diagnostics = new ArrayList<>();
        scans.forEach(scan -> diagnostics.addAll(collectDiagnostics(scan)));
        return diagnostics;
    }

    private static Collection<Diagnostic<? extends JavaFileObject>> collectDiagnostics(Iterable<DiagnosticsScan> scans, Verbosity verbose) {
        List<Diagnostic<? extends JavaFileObject>> diagnostics = new ArrayList<>();

        boolean printProgress = verbose == Verbosity.VERBOSE;

        int current = 1, numberOfScans = Iterables.size(scans);
        for (DiagnosticsScan scan : scans) {
            if (printProgress) {
                System.out.printf("Collecting diagnostics for target [%d / %d]\n", current, numberOfScans);
                ++current;
            }

            try {
                Collection<Diagnostic<? extends JavaFileObject>> scanDiagnostics = collectDiagnostics(scan);
                if (printProgress){
                    System.out.printf("%s had %d files and generated %d alerts\n", scan.name, scan.files.size(), scanDiagnostics.size());
                }

                diagnostics.addAll(scanDiagnostics);
            } catch(Throwable e) {
                System.out.println("Failed to scan a target");
                e.printStackTrace();
                System.out.println(scan);
            }
        }

        return diagnostics;
    }

    private static Iterable<Collection<DiagnosticsScan>> loadScanWalker(CorpusProject project, Iterable<RevCommit> commits) throws IOException {
        switch (project.getBuildSystem()) {
            case Maven:
                return new ScanWalker(project, commits, new MavenProjectScanner());
            case Gradle:
                return new ScanWalker(project, commits, new GradleProjectScanner());
            default:
                throw new IllegalArgumentException("not yet supporting build system of project " + project.getRoot());
        }
    }

    public static Collection<Diagnostic<? extends JavaFileObject>> collectEPDiagnostics(CorpusProject project,
                                                                                        String commitHash) throws IOException {
        return collectEPDiagnostics(project, GitUtils.parseCommit(project.loadRepo(), commitHash));
    }

    public static Collection<Diagnostic<? extends JavaFileObject>> collectEPDiagnostics(CorpusProject project,
                                                                                        RevCommit commit) throws IOException {
        return collectEPDiagnostics(project, commit, Verbosity.SILENT);
    }

    public static Collection<Diagnostic<? extends JavaFileObject>> collectEPDiagnostics(CorpusProject project,
                                                                                        String commitHash,
                                                                                        Verbosity verbose) throws IOException {
        return collectEPDiagnostics(project, GitUtils.parseCommit(project.loadRepo(), commitHash), verbose);
    }

    public static Collection<Diagnostic<? extends JavaFileObject>> collectEPDiagnostics(CorpusProject project,
                                                                                        RevCommit commit,
                                                                                        Verbosity verbose) throws IOException {
        return collectDiagnostics(Iterables.getLast(loadScanWalker(project, ImmutableList.of(commit))), verbose );
    }
}
