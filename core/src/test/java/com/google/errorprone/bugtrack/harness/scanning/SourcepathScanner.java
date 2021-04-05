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

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.errorprone.bugtrack.harness.utils.FileUtils;
import com.google.errorprone.bugtrack.projects.CorpusProject;
import com.google.errorprone.bugtrack.projects.ProjectFile;

import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Finds source files by scanning the sourcepaths of the command line blob. This isn't guarenteed to
 * work for every project since a file in a sourcepath might need flags included by a later blob.
 * For example if one tries to use this approach with the JRuby project it will break.
 */
public class SourcepathScanner implements CmdBlobFilesExtractor {
  private final CorpusProject project;
  private final Set<String> scannedSourcepaths;

  public SourcepathScanner(CorpusProject project) {
    this.project = project;
    this.scannedSourcepaths = new HashSet<>();
  }

  private List<ProjectFile> scanSourcepath(String sourcepath) {
    if (sourcepath.contains(":")) {
      throw new RuntimeException("this method can only scan a single sourcepath");
    }

    if (sourcepath.isEmpty()) {
      return ImmutableList.of();
    }

    return FileUtils.findFilesMatchingGlob(Paths.get(sourcepath), "**/*.java").stream()
        .filter(project::shouldScanFile)
        .map(file -> new ProjectFile(project, file))
        .collect(Collectors.toList());
  }

  @Override
  public List<ProjectFile> extract(List<String> args) {
    int spIndex = args.indexOf("-sourcepath");

    return Splitter.on(':').splitToList(args.get(spIndex + 1)).stream()
        .filter(sp -> !scannedSourcepaths.contains(sp))
        .peek(scannedSourcepaths::add)
        .map(this::scanSourcepath)
        .flatMap(Collection::stream)
        .collect(ImmutableList.toImmutableList());
  }
}
