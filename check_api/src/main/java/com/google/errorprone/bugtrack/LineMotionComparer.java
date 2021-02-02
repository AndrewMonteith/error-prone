/*
 * Copyright 2020 The Error Prone Authors.
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

import com.github.difflib.algorithm.DiffException;
import com.google.common.collect.Iterables;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;

import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static com.google.errorprone.bugtrack.DiagnosticUtils.extractDiagnosticType;

public class LineMotionComparer implements BugComparer {
    private final Repository repo;
    private final RevCommit oldCommit;
    private final RevCommit newCommit;

    private final Map<String, LineMotionTracker> lineTrackers;
    private final List<DiffEntry> diffs;

    public LineMotionComparer(Repository repo, RevCommit oldCommit, RevCommit newCommit) throws GitAPIException, IOException {
        this.repo = repo;
        this.oldCommit = oldCommit;
        this.newCommit = newCommit;
        this.lineTrackers = new HashMap<>();
        this.diffs = GitUtils.computeDiffs(repo, oldCommit, newCommit);
    }

    public LineMotionComparer(Repository repo, String oldCommitHash, String newCommitHash) throws IOException, GitAPIException {
        this(repo,
                GitUtils.parseCommit(repo, oldCommitHash),
                GitUtils.parseCommit(repo, newCommitHash));
    }

    private boolean inSameFile(DatasetDiagnostic oldDiagnostic,
                               DatasetDiagnostic newDiagnostic) {
        String oldPath = DiagnosticUtils.getProjectRelativePath(oldDiagnostic);
        String newPath = DiagnosticUtils.getProjectRelativePath(newDiagnostic);

        if (oldPath.equals(newPath)) {
            return true;
        }

        return diffs.stream().filter(diff -> diff.getChangeType() == DiffEntry.ChangeType.RENAME)
                .anyMatch(diff -> diff.getOldPath().equals(oldPath) && diff.getNewPath().equals(newPath));
    }

    private LineMotionTracker createLineMotionTracker(DatasetDiagnostic oldDiagnostic,
                                                      DatasetDiagnostic newDiagnostic) throws DiffException, IOException {
        List<String> oldText = GitUtils.loadSrcFile(repo, oldCommit, DiagnosticUtils.getProjectRelativePath(oldDiagnostic));
        List<String> newText = GitUtils.loadSrcFile(repo, newCommit, DiagnosticUtils.getProjectRelativePath(newDiagnostic));

        return new LineMotionTracker(oldText, newText);
    }

    private LineMotionTracker getLineMotionTracker(DatasetDiagnostic oldDiagnostic,
                                                   DatasetDiagnostic newDiagnostic) throws DiffException, IOException {
        String oldFile = DiagnosticUtils.getProjectRelativePath(oldDiagnostic);
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
            LineMotionTracker lineTracker = getLineMotionTracker(oldDiagnostic, newDiagnostic);

            Optional<Long> newLine = lineTracker.getNewLine(oldDiagnostic.getLineNumber());

            boolean inSameLine = newLine.isPresent() && newLine.get() == newDiagnostic.getLineNumber();
            boolean inSameColumn = oldDiagnostic.getColumnNumber() == newDiagnostic.getColumnNumber();

            return inSameLine && inSameColumn;
        } catch (IOException | DiffException e) {
            return false;
        }
    }
}
