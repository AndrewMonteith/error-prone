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
import java.util.List;

public class GitPathComparer implements PathsComparer {
  private final Repository repo;
  private final List<DiffEntry> diffs;

  public GitPathComparer(Repository repo, RevCommit oldCommit, RevCommit newCommit)
      throws GitAPIException, IOException {
    this.repo = repo;
    this.diffs = GitUtils.computeDiffs(repo, oldCommit, newCommit);
  }

  public GitPathComparer(Repository repo, String oldCommit, String newCommit)
      throws IOException, GitAPIException {
    this(repo, GitUtils.parseCommit(repo, oldCommit), GitUtils.parseCommit(repo, newCommit));
  }

  private Path makeRelativeToRepo(Path path) {
    Path repoRoot = repo.getDirectory().getParentFile().toPath();

    if (path.startsWith(repoRoot)) {
      return repoRoot.relativize(path);
    } else {
      return path;
    }
  }

  @Override
  public boolean isSameFile(Path oldPath, Path newPath) {
    if (oldPath.equals(newPath)) {
      return true;
    }

    Path relOldPath = makeRelativeToRepo(oldPath);
    Path relNewPath = makeRelativeToRepo(newPath);

    return diffs.stream()
        .filter(diff -> diff.getChangeType() == DiffEntry.ChangeType.RENAME)
        .anyMatch(
            diff ->
                diff.getOldPath().equals(relOldPath.toString())
                    && diff.getNewPath().equals(relNewPath.toString()));
  }
}
