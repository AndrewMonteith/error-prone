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

package com.google.errorprone.bugtrack.hpc;

import com.github.difflib.DiffUtils;
import com.github.difflib.algorithm.DiffException;
import com.github.difflib.algorithm.jgit.HistogramDiff;
import com.github.difflib.patch.AbstractDelta;
import com.github.difflib.patch.Chunk;
import com.github.difflib.patch.Patch;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import com.google.errorprone.bugtrack.*;
import com.google.errorprone.bugtrack.harness.DiagnosticsFile;
import com.google.errorprone.bugtrack.motion.SrcFilePair;
import com.google.errorprone.bugtrack.projects.CorpusProject;
import com.google.googlejavaformat.java.FormatterException;
import org.eclipse.jgit.api.errors.GitAPIException;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ChangedLineFilter {
  private final CorpusProject project;
  private final Collection<DatasetDiagnostic> oldDiags;
  private final Collection<DatasetDiagnostic> newDiags;
  private final SrcFilePairLoader srcFilePairLoader;
  private final PathsComparer pathsComparer;

  ChangedLineFilter(CorpusProject project, DiagnosticsFile oldDiagFile, DiagnosticsFile newDiagFile)
      throws IOException, GitAPIException {
    this.project = project;
    this.oldDiags = oldDiagFile.diagnostics;
    this.newDiags = newDiagFile.diagnostics;
    this.srcFilePairLoader =
        new FormatterSrcFilePairLoader(
            project.loadRepo(), oldDiagFile.commitId, newDiagFile.commitId, null);
    this.pathsComparer =
        new GitPathComparer(project.loadRepo(), oldDiagFile.commitId, newDiagFile.commitId);
  }

  private boolean inChunk(
      DatasetDiagnostic diag,
      Patch<String> patch,
      Function<AbstractDelta<String>, Chunk<String>> getChunk) {
    final long index = diag.getLineNumber() - 1;

    return patch.getDeltas().stream()
        .anyMatch(
            delta -> {
              Chunk<String> chunk = getChunk.apply(delta);

              return chunk.getPosition() <= index && index <= chunk.getPosition() + chunk.size();
            });
  }

  private Results match() throws FormatterException, IOException, DiffException {
    List<DatasetDiagnostic> visibleOld = new ArrayList<>();
    List<DatasetDiagnostic> visibleNew = new ArrayList<>();

    Set<String> oldFiles =
        Sets.newHashSet(Iterables.transform(oldDiags, DatasetDiagnostic::getFileName));

    for (String oldFile : oldFiles) {
      Optional<Path> newPathOpt = pathsComparer.getNewPath(oldFile);

      Collection<DatasetDiagnostic> diagsInFile =
          oldDiags.stream()
              .filter(diag -> diag.getFileName().equals(oldFile))
              .collect(Collectors.toList());

      if (!newPathOpt.isPresent()) { // file is deleted.
        visibleOld.addAll(diagsInFile);
        continue;
      }

      String newPath = newPathOpt.get().toString();

      SrcFilePair srcFilePair = srcFilePairLoader.load(oldFile, newPath);

      Patch<String> diff =
          DiffUtils.diff(
              srcFilePair.oldFile.getLines(),
              srcFilePair.newFile.getLines(),
              new HistogramDiff<>());

      for (DatasetDiagnostic oldDiag : diagsInFile) {
        if (inChunk(oldDiag, diff, AbstractDelta::getSource)) {
          visibleOld.add(oldDiag);
        }
      }

      newDiags.stream()
          .filter(newDiag -> newDiag.getFileName().equals(newPath))
          .forEach(
              newDiag -> {
                if (inChunk(newDiag, diff, AbstractDelta::getTarget)) {
                  visibleNew.add(newDiag);
                }
              });
    }

    Set<String> newFiles =
        Sets.newHashSet(Iterables.transform(newDiags, DatasetDiagnostic::getFileName));
    next_file:
    for (String newFile : newFiles) {
      for (String oldFile : oldFiles) {
        Optional<Path> newFileOpt = pathsComparer.getNewPath(oldFile);
        if (newFileOpt.isPresent() && newFileOpt.get().toString().equals(newFile)) {
          continue next_file;
        }
      }

      newDiags.stream()
          .filter(newDiag -> newDiag.getFileName().equals(newFile))
          .forEach(visibleNew::add);
    }

    return new Results(visibleOld, visibleNew);
  }

  public static Results filter(CorpusProject project, DiagnosticsFile before, DiagnosticsFile after)
      throws IOException, GitAPIException, FormatterException, DiffException {
    return new ChangedLineFilter(project, before, after).match();
  }

  static class Results {
    final ImmutableSet<DatasetDiagnostic> visibleOldDiagnostics;
    final ImmutableSet<DatasetDiagnostic> visibleNewDiagnostics;

    Results(
        Collection<DatasetDiagnostic> visibleOldDiagnostics,
        Collection<DatasetDiagnostic> visibleNewDiagnostics) {
      this.visibleOldDiagnostics = ImmutableSet.copyOf(visibleOldDiagnostics);
      this.visibleNewDiagnostics = ImmutableSet.copyOf(visibleNewDiagnostics);
    }
  }
}
