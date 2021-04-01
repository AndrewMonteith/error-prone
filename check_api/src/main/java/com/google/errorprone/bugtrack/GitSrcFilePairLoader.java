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
import com.google.errorprone.bugtrack.utils.GitUtils;
import com.google.googlejavaformat.java.FormatterException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;

import java.io.IOException;

public class GitSrcFilePairLoader implements SrcFilePairLoader {
  private final Repository repo;
  private final RevCommit oldCommit;
  private final RevCommit newCommit;

  public GitSrcFilePairLoader(Repository repo, RevCommit oldCommit, RevCommit newCommit) {
    this.repo = repo;
    this.oldCommit = oldCommit;
    this.newCommit = newCommit;
  }

  public GitSrcFilePairLoader(Repository repo, String oldCommit, String newCommit)
      throws IOException {
    this(repo, GitUtils.parseCommit(repo, oldCommit), GitUtils.parseCommit(repo, newCommit));
  }

  @Override
  public SrcFilePair load(DatasetDiagnostic oldDiagnostic, DatasetDiagnostic newDiagnostic)
      throws IOException, FormatterException {
    SrcFile oldFile = GitUtils.loadSrcFile(repo, oldCommit, oldDiagnostic.getFileName());
    SrcFile newFile = GitUtils.loadSrcFile(repo, newCommit, newDiagnostic.getFileName());

    return new SrcFilePair(oldFile, newFile);
  }
}
