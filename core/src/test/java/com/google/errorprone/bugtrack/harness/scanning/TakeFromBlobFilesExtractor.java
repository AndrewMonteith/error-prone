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
import com.google.errorprone.bugtrack.projects.CorpusProject;
import com.google.errorprone.bugtrack.projects.ProjectFile;
import com.google.errorprone.bugtrack.projects.ShouldScanUtils;

import java.util.List;

/**
 * Take the source files directly from the blob. This won't work if the files aren't directly
 * included in the blob such as in the jsoup project
 */
public class TakeFromBlobFilesExtractor implements CmdBlobFilesExtractor {
  private final CorpusProject project;

  public TakeFromBlobFilesExtractor(CorpusProject project) {
    this.project = project;
  }

  @Override
  public List<ProjectFile> extract(List<String> cmdlineArgs) {
    return cmdlineArgs.stream()
        .filter(ShouldScanUtils::isJavaFile)
        .filter(project::shouldScanFile)
        .map(file -> new ProjectFile(project, file))
        .collect(ImmutableList.toImmutableList());
  }
}
