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

package com.google.errorprone.bugtrack.hpc;

import com.google.common.collect.ImmutableMap;
import com.google.errorprone.bugtrack.CommitRange;
import com.google.errorprone.bugtrack.harness.LinesChangedCommitFilter;
import com.google.errorprone.bugtrack.harness.ProjectHarness;
import com.google.errorprone.bugtrack.harness.Verbosity;
import com.google.errorprone.bugtrack.projects.*;
import com.google.errorprone.bugtrack.utils.GitUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.util.FS;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class HPCCode {
    private static final Map<String, CorpusProject> projects;

    static {
        Map<String, CorpusProject> projs = new HashMap<>();
        projs.put("jsoup", new JSoupProject());
        projs.put("metrics", new MetricsProject());
        projs.put("checkstyle", new CheckstyleProject());
        projs.put("junit4", new JUnitProject());
        projs.put("dubbo", new DubboProject());
        projs.put("guice", new GuiceProject());
        projs.put("mybatis-3", new MyBatis3Project());
        projs.put("jetty.project", new JettyProject());
        projs.put("hazelcast", new HazelcastProject());

        projects = ImmutableMap.copyOf(projs);
    }

    @Before
    public void initJGit() {
        FS.DETECTED.setGitSystemConfig(Paths.get("/home/am2857/git/etc/gitconfig").toFile());
    }

    private Path getPath(String path1, String... paths) {
        Path p = Paths.get(path1, paths);
        if (!p.toFile().exists()) {
            throw new RuntimeException(p.toString() + " does not exist");
        }
        return p;
    }

    private CorpusProject loadProject() {
        String projectName = System.getProperty("project");
        if (!projects.containsKey(projectName)) {
            throw new RuntimeException("no project named " + projectName);
        }

        return new NewRootProject(projects.get(projectName), getPath(System.getProperty("projDir")));
    }

    @Test
    public void scanCommit() throws IOException {
        CorpusProject project = loadProject();

        String sequenceNumAndCommit = System.getProperty("commit");
        String[] seqNumAndCommitSplit = sequenceNumAndCommit.split(" ");

        RevCommit commit = GitUtils.parseCommit(project.loadRepo(), seqNumAndCommitSplit[1]);
        Path outputFile = getPath(System.getProperty("outputFolder")).resolve(sequenceNumAndCommit);

        System.out.println("Parsing commit " + commit.getName() + " for project " + System.getProperty("project"));
        System.out.println("Saving response into " + outputFile.toString());

        new ProjectHarness(project, Verbosity.VERBOSE).serialiseCommit(commit, outputFile);
    }

    @Test
    public void genCommits() throws GitAPIException, IOException {
        Repository repo = projects.get(System.getProperty("project")).loadRepo();
        CommitRange range = new CommitRange(System.getProperty("oldCommit"), System.getProperty("newCommit"));

        List<RevCommit> filteredCommits = new LinesChangedCommitFilter(new Git(repo), 50)
                .filterCommits(GitUtils.expandCommitRange(repo, range));

        System.out.println("Total commits  " + filteredCommits.size());

        for (int i = 0; i < filteredCommits.size(); ++i) {
            System.out.println(i + " " + filteredCommits.get(i).getName());
        }
    }

    @Test
    public void betweenCommits() throws GitAPIException, IOException {
        CorpusProject project = loadProject();
        CommitRange range = new CommitRange(System.getProperty("oldCommit"), System.getProperty("newCommit"));

        int linesChangedThreshold = System.getProperty("linesChanged") == null ? 50 : Integer.parseInt(System.getProperty("linesChanged"));

        new ProjectHarness(project).serialiseCommits(range,
                new LinesChangedCommitFilter(new Git(project.loadRepo()), linesChangedThreshold),
                getPath(System.getProperty("outputFolder")),
                Integer.parseInt(System.getProperty("offset")));
    }

    public static void main(String[] args) throws GitAPIException, IOException {
        Repository repo = projects.get("jetty.project").loadRepo();
        CommitRange range = new CommitRange("07035b7376c4a64ec8b7509fcce795765cbc9c7b", "217a97b952fe2c7c580880414d78d78455631dc2");

        List<RevCommit> filteredCommits = new LinesChangedCommitFilter(new Git(repo), 1000)
                .filterCommits(GitUtils.expandCommitRange(repo, range));

        System.out.println("Total commits  " + filteredCommits.size());

        for (int i = 0; i < filteredCommits.size(); ++i) {
            System.out.println(i + " " + filteredCommits.get(i).getName());
        }
    }
}
