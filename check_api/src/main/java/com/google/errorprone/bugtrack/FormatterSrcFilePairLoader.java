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

package com.google.errorprone.bugtrack;

import com.google.errorprone.bugtrack.motion.SrcFile;
import com.google.errorprone.bugtrack.motion.SrcFilePair;
import com.google.googlejavaformat.java.FormatterException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;

import java.io.IOException;
import java.nio.file.Path;

public class FormatterSrcFilePairLoader implements SrcFilePairLoader {
  private final SrcFilePairLoader srcFilePairLoader;

  public FormatterSrcFilePairLoader(SrcFilePairLoader srcFilePairLoader) {
    this.srcFilePairLoader = srcFilePairLoader;
  }

  public FormatterSrcFilePairLoader(Repository repo, String oldCommit, String newCommit, TimingInformation timingInformation)
      throws IOException {
    this.srcFilePairLoader = new GitSrcFilePairLoader(repo, oldCommit, newCommit, timingInformation);
  }

  @Override
  public SrcFilePair load(Path oldFile, Path newFile) throws IOException, FormatterException {
    SrcFilePair srcFilePair = srcFilePairLoader.load(oldFile, newFile);

    return new SrcFilePair(
        SrcFile.format(srcFilePair.oldFile.getName(), srcFilePair.oldFile.getSrc()),
        SrcFile.format(srcFilePair.newFile.getName(), srcFilePair.newFile.getSrc()));
  }
}
