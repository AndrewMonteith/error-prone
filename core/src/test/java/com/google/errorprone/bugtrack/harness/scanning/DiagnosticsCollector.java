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
import com.google.errorprone.bugpatterns.javadoc.MissingSummary;
import com.google.errorprone.bugtrack.DatasetDiagnostic;
import com.google.errorprone.bugtrack.harness.Verbosity;
import com.google.errorprone.bugtrack.harness.utils.ListUtils;
import com.google.errorprone.bugtrack.projects.CorpusProject;
import com.google.errorprone.bugtrack.projects.ProjectFile;
import com.google.errorprone.bugtrack.utils.GitUtils;
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
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public final class DiagnosticsCollector {
  private static boolean collectInParallel = true;

  public static void setCollectInParallel(boolean collectInParallel) {
    DiagnosticsCollector.collectInParallel = collectInParallel;
  }

  public static Collection<Diagnostic<? extends JavaFileObject>> collectDiagnostics(
      DiagnosticsScan scan) {
    List<ProjectFile> files =
        scan.files.stream().filter(ProjectFile::exists).collect(Collectors.toList());

    if (files.isEmpty()) {
      return Collections.emptyList();
    }

//    Iterable<BugCheckerInfo> allChecksButVarChecker =
//        Iterables.filter(
//            Iterables.concat(
//                BuiltInCheckerSuppliers.ENABLED_ERRORS,
//                BuiltInCheckerSuppliers.ENABLED_WARNINGS,
//                BuiltInCheckerSuppliers.DISABLED_CHECKS),
//            check -> !check.canonicalName().equals("Var"));
//
//    ScannerSupplier scannerSupplier = ScannerSupplier.fromBugCheckerInfos(allChecksButVarChecker);

    CompilationTestHelper helper =
        CompilationTestHelper.newInstance(MissingSummary.class, DiagnosticsCollector.class);
    files.forEach(projFile -> helper.addSourceFile(projFile.toFile().toPath()));
    helper.setArgs(
        ImmutableList.copyOf(
            Iterables.concat(
                scan.cmdLineArguments,
                ImmutableList.of("-Xjcov", "-XDshould-stop.ifError=LOWER"))));

    if (!helper.compile().isOK()) {
      String output = helper.getOutput();
      if (!output.trim().isEmpty()) {
        System.out.println("non-ok result");
        System.out.println(scan);
        System.out.println(output);
      }
    }

    Collection<Diagnostic<? extends JavaFileObject>> collectedDiagnostics =
        ListUtils.distinct(helper.getDiagnostics());

    if (collectedDiagnostics.isEmpty() && files.size() > 5) {
      // Seems suspicious
      System.out.println("Weird following scan had 0 files");
      System.out.println(scan);
    }

    return ListUtils.distinct(collectedDiagnostics);
  }

  private static Collection<Diagnostic<? extends JavaFileObject>> collectDiagnostics(
      Iterable<DiagnosticsScan> scans) {
    List<Diagnostic<? extends JavaFileObject>> diagnostics = new ArrayList<>();
    scans.forEach(scan -> diagnostics.addAll(collectDiagnostics(scan)));
    return diagnostics;
  }

  private static Collection<Diagnostic<? extends JavaFileObject>> scanInParallel(
      Iterable<DiagnosticsScan> scans, Verbosity verbose) {
    // Split scans apart to ensure each one has at most files
    final Collection<DiagnosticsScan> partitionedScans =
        StreamSupport.stream(scans.spliterator(), false)
            .map(
                scan -> {
                  if (scan.files.size() <= 200) {
                    return ImmutableList.of(scan);
                  } else {
                    return DiagnosticsScanUtil.chunkScan(scan, 200);
                  }
                })
            .flatMap(Collection::stream)
            .collect(Collectors.toList());

    // so using parallel streams doesn't work since it's about data parallelism so since scans
    // doesn't have alot
    // of items in they're not chunked at all. This code is not idiomatic but hopefully works.
    ExecutorService executor =
        Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

    List<Callable<Collection<Diagnostic<? extends JavaFileObject>>>> scanTasks = new ArrayList<>();
    partitionedScans.forEach(
        scan ->
            scanTasks.add(
                () -> {
                  System.out.printf("Scanning %s with %d files\n", scan.name, scan.files.size());
                  try {
                    Collection<Diagnostic<? extends JavaFileObject>> diagnostics =
                        collectDiagnostics(scan);
                    System.out.printf(
                        "Finished scanning %s with %d files and got %d diagnostics\n",
                        scan.name, scan.files.size(), diagnostics.size());
                    return diagnostics;
                  } catch (Throwable e) {
                    System.out.println("Failed to scan target " + scan.name);
                    e.printStackTrace();
                    System.out.println(scan);
                    return ImmutableList.of();
                  }
                }));

    try {
      Collection<Diagnostic<? extends JavaFileObject>> result = new ArrayList<>();

      List<Future<Collection<Diagnostic<? extends JavaFileObject>>>> results =
          executor.invokeAll(scanTasks);
      for (Future<Collection<Diagnostic<? extends JavaFileObject>>> future : results) {
        result.addAll(future.get());
      }

      return result;
    } catch (InterruptedException | ExecutionException e) {
      throw new RuntimeException(e);
    } finally {
      executor.shutdown();
    }
  }

  private static Collection<Diagnostic<? extends JavaFileObject>> scanInSerial(
      Iterable<DiagnosticsScan> scans, Verbosity verbose) {
    List<Diagnostic<? extends JavaFileObject>> diagnostics = new ArrayList<>();

    boolean printProgress = true;

    int current = 1, numberOfScans = Iterables.size(scans);
    if (printProgress) {
      System.out.println("Found " + numberOfScans + " scans");
    }
    for (DiagnosticsScan scan : scans) {
      if (printProgress) {
        System.out.printf("Collecting diagnostics for target [%d / %d]\n", current, numberOfScans);
        ++current;
      }

      try {
        Collection<Diagnostic<? extends JavaFileObject>> scanDiagnostics = collectDiagnostics(scan);
        if (printProgress) {
          System.out.printf(
              "%s had %d files and generated %d alerts\n",
              scan.name, scan.files.size(), scanDiagnostics.size());
        }

        diagnostics.addAll(scanDiagnostics);
      } catch (Throwable e) {
        System.out.println("Failed to scan target " + scan.name);
        e.printStackTrace();
        System.out.println(scan);
      }
    }

    return diagnostics;
  }

  public static Collection<Diagnostic<? extends JavaFileObject>> collectDiagnosticsFromScans(
      Iterable<DiagnosticsScan> scans, Verbosity verbose) {
    if (collectInParallel) {
      return scanInParallel(scans, verbose);
    } else {
      return scanInSerial(scans, verbose);
    }
  }

  private static Iterable<Collection<DiagnosticsScan>> loadScanWalker(
      CorpusProject project, Iterable<RevCommit> commits) throws IOException {
    switch (project.getBuildSystem()) {
      case Maven:
        return new ScanWalker(project, commits, new MavenProjectScanner());
      case Gradle:
        return new ScanWalker(project, commits, new GradleProjectScanner());
      default:
        throw new IllegalArgumentException(
            "not yet supporting build system of project " + project.getRoot());
    }
  }

  public static Collection<Diagnostic<? extends JavaFileObject>> collectEPDiagnostics(
      CorpusProject project, String commitHash) throws IOException {
    return collectEPDiagnostics(project, GitUtils.parseCommit(project.loadRepo(), commitHash));
  }

  public static Collection<Diagnostic<? extends JavaFileObject>> collectEPDiagnostics(
      CorpusProject project, RevCommit commit) throws IOException {
    return collectEPDiagnostics(project, commit, Verbosity.SILENT);
  }

  public static Collection<Diagnostic<? extends JavaFileObject>> collectEPDiagnostics(
      CorpusProject project, String commitHash, Verbosity verbose) throws IOException {
    return collectEPDiagnostics(
        project, GitUtils.parseCommit(project.loadRepo(), commitHash), verbose);
  }

  public static Collection<Diagnostic<? extends JavaFileObject>> collectEPDiagnostics(
      CorpusProject project, RevCommit commit, Verbosity verbose) throws IOException {
    return collectDiagnosticsFromScans(
        Iterables.getLast(loadScanWalker(project, ImmutableList.of(commit))), verbose);
  }

  public static Collection<DatasetDiagnostic> collectDatasetDiagnosics(DiagnosticsScan scan) {
    return collectDiagnostics(scan).stream()
        .map(DatasetDiagnostic::new)
        .collect(Collectors.toList());
  }
}
