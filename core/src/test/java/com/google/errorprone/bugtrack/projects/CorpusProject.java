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

package com.google.errorprone.bugtrack.projects;

import com.google.errorprone.bugtrack.harness.scanning.CheckoutForwader;
import com.google.errorprone.bugtrack.harness.scanning.CmdBlobFilesExtractor;
import com.google.errorprone.bugtrack.harness.scanning.CommitForwarder;
import com.google.errorprone.bugtrack.harness.scanning.TakeFromBlobFilesExtractor;
import com.google.errorprone.bugtrack.utils.GitUtils;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;

import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

public interface CorpusProject {
  @NotNull
  Path getRoot();

  boolean shouldScanFile(Path file);

  default boolean shouldScanFile(String s) {
    return shouldScanFile(Paths.get(s));
  }

  BuildSystem getBuildSystem();

  default Repository loadRepo() {
    return GitUtils.loadRepo(getRoot());
  }

  default CommitForwarder getForwarder() {
    return new CheckoutForwader();
  }

  default CmdBlobFilesExtractor getFilesExtractor() {
    return new TakeFromBlobFilesExtractor(this);
  }

  enum BuildSystem {
    Maven,
    Gradle,
    Other
  }
}
