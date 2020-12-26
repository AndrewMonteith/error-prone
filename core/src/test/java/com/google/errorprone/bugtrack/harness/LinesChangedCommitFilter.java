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

package com.google.errorprone.bugtrack.harness;

import com.google.common.collect.Iterables;
import com.google.common.io.Files;
import com.google.errorprone.bugtrack.GitUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.patch.FileHeader;
import org.eclipse.jgit.revwalk.RevCommit;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class LinesChangedCommitFilter implements CommitRangeFilter {
    private final int javaLinesChangedThreshold;
    private final Git git;

    public LinesChangedCommitFilter(Git git, int javaLinesChangedThreshold) {
        this.javaLinesChangedThreshold = javaLinesChangedThreshold;
        this.git = git;
    }

    private int computeJavaLinesChanged(Git git, RevCommit earlierCommit, RevCommit laterCommit) throws IOException, GitAPIException {
        List<DiffEntry> diffs = GitUtils.computeDiffs(git, earlierCommit, laterCommit);

        DiffFormatter formatter = new DiffFormatter(null);
        formatter.setRepository(git.getRepository());
        formatter.setDetectRenames(true);

        int totalLinesChanged = 0
;
        for (DiffEntry diff : diffs) {
            if (!Files.getFileExtension(diff.getNewPath()).equals("java")) {
                continue;
            }

            FileHeader header = formatter.toFileHeader(diff);

            totalLinesChanged += header.toEditList().stream()
                    .mapToInt(edit -> edit.getLengthA() + edit.getLengthB())
                    .sum();
        }

        return totalLinesChanged;
    }

    @Override
    public List<RevCommit> filterCommits(List<RevCommit> commits) throws IOException, GitAPIException {
        List<RevCommit> filteredCommits = new ArrayList<RevCommit>();
        filteredCommits.add(commits.get(0));

        RevCommit currentCommit = commits.get(0);
        for (int i = 1; i < commits.size(); ++i) {
            RevCommit commit = commits.get(i);

            if (computeJavaLinesChanged(git, currentCommit, commit) >= javaLinesChangedThreshold) {
                filteredCommits.add(commit);
                currentCommit = commit;
            }
        }

        if (Iterables.getLast(filteredCommits) != Iterables.getLast(commits)) {
            filteredCommits.add(Iterables.getLast(commits));
        }

        return filteredCommits;
    }
}
