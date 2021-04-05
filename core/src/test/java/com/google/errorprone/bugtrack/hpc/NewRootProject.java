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

import com.google.errorprone.bugtrack.harness.scanning.CmdBlobFilesExtractor;
import com.google.errorprone.bugtrack.harness.scanning.CommitForwarder;
import com.google.errorprone.bugtrack.projects.CorpusProject;

import java.nio.file.Path;

public class NewRootProject implements CorpusProject {
  private final CorpusProject project;
  private final Path newRoot;

  public NewRootProject(CorpusProject project, Path newRoot) {
    this.project = project;
    this.newRoot = newRoot;
  }

  @Override
  public Path getRoot() {
    return newRoot;
  }

  @Override
  public boolean shouldScanFile(Path file) {
    return project.shouldScanFile(file);
  }

  @Override
  public BuildSystem getBuildSystem() {
    return project.getBuildSystem();
  }

  @Override
  public CmdBlobFilesExtractor getFilesExtractor() {
    return project.getFilesExtractor();
  }

  @Override
  public CommitForwarder getForwarder() {
    return project.getForwarder();
  }
}
