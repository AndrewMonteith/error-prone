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

package com.google.errorprone;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.errorprone.bugtrack.GitSrcFilePairLoader;
import com.google.errorprone.bugtrack.SrcFilePairLoader;
import com.google.errorprone.bugtrack.motion.SrcFile;
import com.google.errorprone.bugtrack.motion.SrcFilePair;
import com.google.errorprone.bugtrack.utils.GitUtils;
import com.google.googlejavaformat.java.FormatterException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class FormattedRepoSrcPairLoader implements SrcFilePairLoader {
  private SrcFilePairLoader srcFilePairLoader;

  private static final Splitter COMMIT_MAP_SPLITTER = Splitter.on(" -> ");

  private static ImmutableMap<String, String> loadCommitMapping(Path formattedProjectRepo)
      throws IOException {
    Path commitMapFile = formattedProjectRepo.resolve("commit_map");
    if (!commitMapFile.toFile().exists()) {
      throw new RuntimeException("no commit map for " + formattedProjectRepo);
    }

    return Files.readAllLines(commitMapFile).stream()
        .collect(
            ImmutableMap.toImmutableMap(
                line -> Iterables.get(COMMIT_MAP_SPLITTER.split(line), 0),
                line -> Iterables.get(COMMIT_MAP_SPLITTER.split(line), 1)));
  }

  public FormattedRepoSrcPairLoader(
      Path formattedProjectRepo, RevCommit oldCommit, RevCommit newCommit) throws IOException {
    Repository repo = GitUtils.loadRepo(formattedProjectRepo);
    ImmutableMap<String, String> commitMapping = loadCommitMapping(formattedProjectRepo);

    this.srcFilePairLoader =
        new GitSrcFilePairLoader(
            repo, commitMapping.get(oldCommit.getName()), commitMapping.get(newCommit.getName()));
  }

  public FormattedRepoSrcPairLoader(Repository origRepo, Path formattedRepo, String oldCommit, String newCommit) throws IOException {
    this(formattedRepo, GitUtils.parseCommit(origRepo, oldCommit), GitUtils.parseCommit(origRepo, newCommit));
  }

  @Override
  public SrcFilePair load(Path oldFile, Path newFile) throws IOException, FormatterException {
    return srcFilePairLoader.load(oldFile, newFile);
  }
}
