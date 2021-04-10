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

import com.google.errorprone.bugtrack.utils.GitUtils;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class GitPathComparer implements PathsComparer {
  private final Map<Path, Path> renames;
  private final Set<Path> deletions;

  public GitPathComparer(Repository repo, RevCommit oldCommit, RevCommit newCommit)
      throws GitAPIException, IOException {
    Collection<DiffEntry> diffs = GitUtils.computeDiffs(repo, oldCommit, newCommit);
    this.renames =
        diffs.stream()
            .filter(diff -> diff.getChangeType() == DiffEntry.ChangeType.RENAME)
            .collect(
                Collectors.toMap(
                    entry -> Paths.get(entry.getOldPath()),
                    entry -> Paths.get(entry.getNewPath())));
    this.deletions =
        diffs.stream()
            .filter(diff -> diff.getChangeType() == DiffEntry.ChangeType.DELETE)
            .map(diff -> Paths.get(diff.getOldPath()))
            .collect(Collectors.toSet());
  }

  public GitPathComparer(Repository repo, String oldCommit, String newCommit)
      throws IOException, GitAPIException {
    this(repo, GitUtils.parseCommit(repo, oldCommit), GitUtils.parseCommit(repo, newCommit));
  }

  @Override
  public Optional<Path> getNewPath(Path oldPath) {
    if (deletions.contains(oldPath)) {
      return Optional.empty();
    }
    return Optional.of(renames.getOrDefault(oldPath, oldPath));
  }
}
