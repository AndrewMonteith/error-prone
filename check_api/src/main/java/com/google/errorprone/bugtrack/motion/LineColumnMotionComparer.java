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

import com.github.difflib.algorithm.DiffException;
import com.google.errorprone.bugtrack.BugComparer;
import com.google.errorprone.bugtrack.DatasetDiagnostic;
import com.google.errorprone.bugtrack.DiagnosticUtils;
import com.google.errorprone.bugtrack.GitUtils;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class LineColumnMotionComparer implements BugComparer {
    private final Repository repo;
    private final RevCommit oldCommit;
    private final RevCommit newCommit;

    private final Map<String, LineMotionTracker<String>> lineTrackers;
    private final List<DiffEntry> diffs;

    public LineColumnMotionComparer(Repository repo, RevCommit oldCommit, RevCommit newCommit) throws GitAPIException, IOException {
        this.repo = repo;
        this.oldCommit = oldCommit;
        this.newCommit = newCommit;
        this.lineTrackers = new HashMap<>();
        this.diffs = GitUtils.computeDiffs(repo, oldCommit, newCommit);
    }

    public LineColumnMotionComparer(Repository repo, String oldCommitHash, String newCommitHash) throws IOException, GitAPIException {
        this(repo,
                GitUtils.parseCommit(repo, oldCommitHash),
                GitUtils.parseCommit(repo, newCommitHash));
    }

    private boolean inSameFile(DatasetDiagnostic oldDiagnostic,
                               DatasetDiagnostic newDiagnostic) {
        String oldPath = oldDiagnostic.getFileName();
        String newPath = newDiagnostic.getFileName();

        if (oldPath.equals(newPath)) {
            return true;
        }

        return diffs.stream()
                .filter(diff -> diff.getChangeType() == DiffEntry.ChangeType.RENAME)
                .anyMatch(diff -> diff.getOldPath().equals(oldPath) && diff.getNewPath().equals(newPath));
    }

    private LineMotionTracker<String> createLineMotionTracker(DatasetDiagnostic oldDiagnostic,
                                                      DatasetDiagnostic newDiagnostic) throws DiffException, IOException {
        List<String> oldText = GitUtils.loadSrcFile(repo, oldCommit, oldDiagnostic.getFileName());
        List<String> newText = GitUtils.loadSrcFile(repo, newCommit, newDiagnostic.getFileName());

        return LineMotionTracker.newLineCharsTracker(oldText, newText);
    }

    private LineMotionTracker<String> getLineMotionTracker(DatasetDiagnostic oldDiagnostic,
                                                   DatasetDiagnostic newDiagnostic) throws DiffException, IOException {
        String oldFile = oldDiagnostic.getFileName();
        if (!lineTrackers.containsKey(oldFile)) {
            lineTrackers.put(oldFile, createLineMotionTracker(oldDiagnostic, newDiagnostic));
        }

        return lineTrackers.get(oldFile);
    }

    @Override
    public boolean areSame(DatasetDiagnostic oldDiagnostic,
                           DatasetDiagnostic newDiagnostic) {
        if (!(inSameFile(oldDiagnostic, newDiagnostic) && oldDiagnostic.isSameType(newDiagnostic))) {
            return false;
        }

        try {
            LineMotionTracker<String> lineTracker = getLineMotionTracker(oldDiagnostic, newDiagnostic);

            Optional<Long> newLine = lineTracker.getNewLine(oldDiagnostic.getLineNumber());

            boolean inSameLine = newLine.isPresent() && newLine.get() == newDiagnostic.getLineNumber();
            boolean inSameColumn = oldDiagnostic.getColumnNumber() == newDiagnostic.getColumnNumber();

            return inSameLine && inSameColumn;
        } catch (IOException | DiffException e) {
            return false;
        }
    }
}
