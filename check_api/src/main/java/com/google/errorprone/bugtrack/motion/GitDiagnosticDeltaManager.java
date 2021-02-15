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

package com.google.errorprone.bugtrack.motion;

import com.google.errorprone.bugtrack.DatasetDiagnostic;
import com.google.errorprone.bugtrack.utils.GitUtils;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;

import java.io.IOException;
import java.util.List;

public class GitDiagnosticDeltaManager implements DiagnosticsDeltaManager {
    private final Repository repo;
    private final RevCommit oldCommit;
    private final RevCommit newCommit;
    private final List<DiffEntry> diffs;

    public GitDiagnosticDeltaManager(Repository repo, RevCommit oldCommit, RevCommit newCommit) throws GitAPIException, IOException {
        this.repo = repo;
        this.oldCommit = oldCommit;
        this.newCommit = newCommit;
        this.diffs = GitUtils.computeDiffs(repo, oldCommit, newCommit);
    }

    public GitDiagnosticDeltaManager(Repository repo, String oldCommit, String newCommit) throws IOException, GitAPIException {
        this(repo,
                GitUtils.parseCommit(repo, oldCommit),
                GitUtils.parseCommit(repo, newCommit));
    }

    @Override
    public boolean inSameFile(DatasetDiagnostic oldDiagnostic, DatasetDiagnostic newDiagnostic) {
        String oldPath = oldDiagnostic.getFileName();
        String newPath = newDiagnostic.getFileName();

        if (oldPath.equals(newPath)) {
            return true;
        }

        return diffs.stream()
                .filter(diff -> diff.getChangeType() == DiffEntry.ChangeType.RENAME)
                .anyMatch(diff -> diff.getOldPath().equals(oldPath) && diff.getNewPath().equals(newPath));
    }

    @Override
    public SrcFilePair loadFilesBetweenDiagnostics(DatasetDiagnostic oldDiagnostic, DatasetDiagnostic newDiagnostic) throws IOException {
        SrcFile oldFile = GitUtils.loadSrcFile(repo, oldCommit, oldDiagnostic.getFileName());
        SrcFile newFile = GitUtils.loadSrcFile(repo, newCommit, newDiagnostic.getFileName());

        return new SrcFilePair(oldFile, newFile);
    }
}
