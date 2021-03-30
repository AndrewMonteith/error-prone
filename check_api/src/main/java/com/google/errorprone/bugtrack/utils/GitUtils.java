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

package com.google.errorprone.bugtrack.utils;

import com.google.common.base.Splitter;
import com.google.errorprone.bugtrack.CommitRange;
import com.google.errorprone.bugtrack.DatasetDiagnostic;
import com.google.errorprone.bugtrack.motion.SrcFile;
import com.google.googlejavaformat.java.FormatterException;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.PathFilter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;

public class GitUtils {
    private static boolean isMatchingCommit(RevCommit commit, String commitId) {
        return commit.getName().startsWith(commitId);
    }

    public static List<RevCommit> expandCommitRange(Repository repo, CommitRange range) throws GitAPIException, IOException {
        Deque<RevCommit> commits = new LinkedList<>();

        try (RevWalk walk = new RevWalk(repo)) {
            walk.markStart(walk.parseCommit(ObjectId.fromString(range.finalCommit)));
            for (RevCommit commit : walk) {
                if (isMatchingCommit(commit, range.startCommit)) {
                    break;
                }
                commits.addFirst(commit);
            }

            commits.addFirst(walk.parseCommit(ObjectId.fromString(range.startCommit)));
        }

        return new ArrayList<>(commits);
    }

    public static RevCommit parseCommit(Repository repo, String commit) throws IOException {
        try (RevWalk walker = new RevWalk(repo)) {
            return walker.parseCommit(ObjectId.fromString(commit));
        }
    }

    public static List<DiffEntry> computeDiffs(Repository repo, RevCommit olderCommit, RevCommit newerCommit) throws GitAPIException, IOException {
        DiffFormatter formatter = new DiffFormatter(null);
        formatter.setRepository(repo);
        formatter.setDetectRenames(true);

        return formatter.scan(olderCommit, newerCommit);
    }

    private static String makePathRelativeToRepo(Repository repo, String path) {
        String pathToProject = repo.getDirectory().getParentFile().getAbsolutePath() + "/";

        return path.startsWith(pathToProject) ? path.replaceFirst(pathToProject, "") : path;
    }

    public static SrcFile loadSrcFile(Repository repo, RevCommit commit, String path) throws IOException, FormatterException {
        RevTree tree = commit.getTree();

        String relativePath = makePathRelativeToRepo(repo, path);

        try (TreeWalk treeWalk = new TreeWalk(repo)) {
            treeWalk.addTree(tree);
            treeWalk.setRecursive(true);
            treeWalk.setFilter(PathFilter.create(relativePath));

            treeWalk.next();
            ObjectId oId = treeWalk.getObjectId(0);

            if (oId == ObjectId.zeroId()) {
                throw new IOException("could not find " + relativePath + " inside the commit " + commit.getName());
            }

            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            repo.open(oId).copyTo(stream);

            return new SrcFile(path, stream.toString());
        }
    }

    public static String loadJavaLine(Repository repo, RevCommit commit, DatasetDiagnostic diag) throws IOException, FormatterException {
        return loadSrcFile(repo, commit, diag.getFileName()).getLines().get((int)diag.getLineNumber()-1);
    }

    public static List<DiffEntry> computeDiffs(Git git, RevCommit olderCommit, RevCommit newerCommit) throws GitAPIException, IOException {
        return computeDiffs(git.getRepository(), olderCommit, newerCommit);
    }

    public static void checkoutMaster(Repository repo) {
        try {
            new Git(repo).checkout().setName("master").call();
        } catch (GitAPIException e) {
            e.printStackTrace();
        }
    }

    public static void checkoutRepo(Repository repo, String commitId) {
        try {
            new Git(repo).checkout().setName(commitId).call();
        } catch (GitAPIException e) {
            e.printStackTrace();
        }
    }

    public static void checkoutFiles(Repository repo) {
        try {
            new Git(repo).checkout().setAllPaths(true).call();
        } catch (GitAPIException e) {
            e.printStackTrace();
        }
    }
}
