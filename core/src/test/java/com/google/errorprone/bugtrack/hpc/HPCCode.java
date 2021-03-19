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
import com.google.errorprone.bugtrack.GitPathComparer;
import com.google.errorprone.bugtrack.GitSrcFilePairLoader;
import com.google.errorprone.bugtrack.harness.LinesChangedCommitFilter;
import com.google.errorprone.bugtrack.harness.ProjectHarness;
import com.google.errorprone.bugtrack.harness.Verbosity;
import com.google.errorprone.bugtrack.harness.evaluating.BugComparerExperiment;
import com.google.errorprone.bugtrack.harness.evaluating.IntRanges;
import com.google.errorprone.bugtrack.harness.evaluating.LiveDatasetFilePairLoader;
import com.google.errorprone.bugtrack.harness.evaluating.MissedLikelihoodCalculatorFactory;
import com.google.errorprone.bugtrack.projects.*;
import com.google.errorprone.bugtrack.utils.GitUtils;
import com.google.errorprone.bugtrack.utils.ProjectFiles;
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

import static com.google.errorprone.bugtrack.harness.evaluating.BugComparerExperiment.withGit;
import static com.google.errorprone.bugtrack.motion.trackers.DPTrackerConstructorFactory.*;
import static com.google.errorprone.bugtrack.motion.trackers.DPTrackerConstructorFactory.newIJMStartAndEndTracker;

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
        projs.put("zxing", new ZXingProject());

        projects = ImmutableMap.copyOf(projs);
    }

    @Before
    public void initJGit() {
        FS.DETECTED.setGitSystemConfig(Paths.get("/rds/user/am2857/hpc-work/java-corpus/git/etc/gitconfig").toFile());
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

        List<RevCommit> filteredCommits = new LinesChangedCommitFilter(new Git(repo), Integer.valueOf(System.getProperty("linesChanged")))
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

        System.out.println("Lines changed threshold "+ linesChangedThreshold);
        System.out.println("Number of cores " + Runtime.getRuntime().availableProcessors());

        new ProjectHarness(project).serialiseCommits(range,
                new LinesChangedCommitFilter(new Git(project.loadRepo()), linesChangedThreshold),
                getPath(System.getProperty("outputFolder")),
                Integer.parseInt(System.getProperty("offset")));
    }

    @Test
    public void genDubboComparisons() throws Exception {
        CorpusProject project = new MyBatis3Project();
        Path diagFolders = ProjectFiles.get("diagnostics/dubbo");
        IntRanges validSeqFiles = IntRanges.include(0, 158).excludeRange(71, 73);

        BugComparerExperiment.forProject(project)
                .withData(LiveDatasetFilePairLoader.inSeqNumRange(diagFolders, validSeqFiles))
                .comparePaths(withGit(project, GitPathComparer::new))
                .loadDiags(withGit(project, GitSrcFilePairLoader::new))
                .makeBugComparer1(any(newTokenizedLineTracker(), newIJMStartPosTracker()))
                .makeBugComparer2(any(newTokenizedLineTracker(), newIJMStartAndEndTracker()))
                .findMissedTrackings(MissedLikelihoodCalculatorFactory.diagLineSrcOverlap())
                .trials(Integer.parseInt(System.getProperty("trials")))
                .run(ProjectFiles.get("comparisons/dubbo").toString());
    }

    public static void main(String[] args) throws GitAPIException, IOException {
        Repository repo = new JenkinsProject().loadRepo();
        CommitRange range = new CommitRange("a544cb79f8d102228fbb5929fad44712a8c9fc76", "7eae00a5a5c82bfc34c066ff8bc2db24d0aa2e52");

        System.out.println(GitUtils.expandCommitRange(repo, range).size());
        
        List<RevCommit> filteredCommits = new LinesChangedCommitFilter(new Git(repo), 1000)
                .filterCommits(GitUtils.expandCommitRange(repo, range));

        System.out.println("Total commits  " + filteredCommits.size());

//        for (int i = 0; i < filteredCommits.size(); ++i) {
//            System.out.println(i + " " + filteredCommits.get(i).getName());
//        }
    }
}