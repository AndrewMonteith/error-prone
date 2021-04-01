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

import com.google.common.collect.ImmutableList;
import com.google.errorprone.bugtrack.harness.DiagnosticsFile;
import com.google.errorprone.bugtrack.harness.JavaLinesChangedFilter;
import com.google.errorprone.bugtrack.harness.ProjectHarness;
import com.google.errorprone.bugtrack.harness.Verbosity;
import com.google.errorprone.bugtrack.harness.evaluating.*;
import com.google.errorprone.bugtrack.harness.matching.DiagnosticsMatcher;
import com.google.errorprone.bugtrack.harness.matching.GitCommitMatcher;
import com.google.errorprone.bugtrack.harness.matching.MatchResults;
import com.google.errorprone.bugtrack.harness.scanning.DiagnosticsCollector;
import com.google.errorprone.bugtrack.motion.DiagnosticPositionMotionComparer;
import com.google.errorprone.bugtrack.projects.*;
import com.google.errorprone.bugtrack.utils.GitUtils;
import com.google.errorprone.bugtrack.utils.ProjectFiles;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static com.google.errorprone.bugtrack.harness.evaluating.BugComparerExperiment.withGit;
import static com.google.errorprone.bugtrack.motion.trackers.DPTrackerConstructorFactory.*;

@RunWith(JUnit4.class)
public class ProjectTests {
    private void assertFindsDiagnostics(CorpusProject project, String commitHash) throws IOException {
        // WHEN:
        GitUtils.checkoutMaster(project.loadRepo());

        Collection<Diagnostic<? extends JavaFileObject>> diagnostics =
                DiagnosticsCollector.collectEPDiagnostics(project, commitHash, Verbosity.VERBOSE);

        // THEN:
        System.out.println(diagnostics.size());
        Assert.assertTrue(diagnostics.size() > 0);
    }

    @Test
    public void canScanJSoup() throws IOException {
        assertFindsDiagnostics(new JSoupProject(),
                "79496d8d047f2d0774e0ad9d8169a021cb828fab");
    }

    @Test
    public void canScanGuice() throws IOException {
        assertFindsDiagnostics(new GuiceProject(),
                "dafa4b0bec4e7ec5e1df75e3fb9a2fdf4920921a");
    }

    @Test
    public void canScanMyBatis3() throws IOException {
        assertFindsDiagnostics(new MyBatis3Project(),
                "b2c8a2cc9e8daf43817d0151bbf3826ae884df40");
    }

    @Test
    public void canSerialiseDiagnostics2() throws IOException {
        assertFindsDiagnostics(new GuiceProject(),
                "dafa4b0bec4e7ec5e1df75e3fb9a2fdf4920921a");
    }

    @Test
    public void canSerialiseDiagnostics() throws IOException {
        // Will only pass on defaultChecks

        // GIVEN:
        CorpusProject jsoup = new JSoupProject();
        RevCommit commit = GitUtils.parseCommit(jsoup.loadRepo(), "468c5369b52ca45de3c7e54a3d2ddae352495851");
        Path output = Paths.get("/home/monty/IdeaProjects/java-corpus/jsoup_output");

        // WHEN:
        new ProjectHarness(jsoup, Verbosity.VERBOSE).serialiseCommit(commit, output);

        // THEN:
        Assert.assertEquals(376, DiagnosticsFile.load(jsoup, output).diagnostics.size());
    }

    @Test
    public void serialiseDiagnostics() throws IOException, GitAPIException, InterruptedException {
        CorpusProject project = new JSoupProject();
        CommitRange range = new CommitRange("f1110a9021c2caa28cbe3177c0c3a0f5ae326eb4", "ae9a18c9e1382b5d8bad14d09279eda725490c25");

        new ProjectHarness(project).serialiseCommits(range,
                new JavaLinesChangedFilter(new Git(project.loadRepo()), 25),
                Paths.get("/home/monty/IdeaProjects/java-corpus/diagnostics/jsoup25"));
    }

    @Test
    public void canScanRestAssured() throws IOException {
        // GIVEN:
        assertFindsDiagnostics(new RestAssuredProject(),
                "9eac8ca6c1d97f8d5a910e553522bdb40752364a");
    }

    @Test
    public void canScanDubbo() throws IOException {
        assertFindsDiagnostics(new DubboProject(),
                "47fa631c091a94b427881ce3371555fac8c1226f");
    }

    @Test
    public void canScanMetrics() throws IOException {
        assertFindsDiagnostics(new MetricsProject(),
                "e7a0370621aead1b9069e88a994af4b5e8bca25a");
    }

    @Test
    public void canScanJUnit() throws IOException {
        // Note 9b061ea8c96fa6cba0ac9d7cfd5e8ffbd030b34a does not compile
        assertFindsDiagnostics(new JUnitProject(),
                "138d278012f9d647f983f59c3ede954ea111bdd5");
    }

    @Test
    public void canScanSpringFramework() throws IOException {
        assertFindsDiagnostics(new SpringFrameworkProject(),
                "1cb9f2c7b2a25daae014349ba3df7823b3584171");
    }

    @Test
    public void canScanHibernate() throws IOException {
        assertFindsDiagnostics(new HibernateProject(),
                "6cead49fec02761cb616fe8ea20134b90115f8b5");
    }

    @Test
    public void canScanOkHttp() throws IOException {
        assertFindsDiagnostics(new OkHttpProject(),
                "5d72f8980d841173dd0d52ae0422e8ea04ef1e09");
    }

    @Test
    public void canScanCheckstyle() throws IOException {
        assertFindsDiagnostics(new CheckstyleProject(), "23cf0bed6c36b9f31143e28bca000b290b23c799");
    }

    @Test
    public void canScanMcMMO() throws IOException {
        assertFindsDiagnostics(new McMMOProject(), "6cec253243bba1f661e2374cd0cbe18bb84721aa");
    }

    @Test
    public void canScanOrient() throws IOException {
        assertFindsDiagnostics(new OrientDBProject(), "a93486bdef194e8116afd2576358951f0eb702dd");
    }

    @Test
    public void canScanCobertura() throws IOException {
        assertFindsDiagnostics(new CoberturaProject(), "2be4bbb667f41a47143153def1b76880dc85851a");
    }

    @Test
    public void detectsChangesInDiagnosticsInMavenProject() throws IOException {
        // Test will only pass if scanning with default diagnostic checkers

        // GIVEN:
        CorpusProject project = new JSoupProject();
        List<String> commits = ImmutableList.of(
                "3c37bffed94c19c5f500217eb568bcdf394be64e",
                "afd73606a90909444e1c443b555dae7b71e6a5a0",
                "724b2c5bf576cbd548738756bfe5f7a7b90c6239",
                "690d601950bf44fc84dcc711b2ef265f9542df62"
        );

        // WHEN:
        List<Integer> numberOfDiagnostics = new ArrayList<>();
        new ProjectHarness(project, Verbosity.VERBOSE).forEachCommitIdWithDiagnostics(
                commits, (commit, diagnostics) -> numberOfDiagnostics.add(diagnostics.size()));

        // THEN:
        Assert.assertEquals(4, numberOfDiagnostics.size());
        Assert.assertEquals((int) numberOfDiagnostics.get(0), 376);
        Assert.assertEquals((int) numberOfDiagnostics.get(1), 376);
        Assert.assertEquals((int) numberOfDiagnostics.get(2), 378);
        Assert.assertEquals((int) numberOfDiagnostics.get(3), 378);
    }


    @Test
    public void example_DetectingNewBug() throws IOException, GitAPIException {
        // GIVEN:
        CorpusProject project = new JSoupProject();

        String oldCommit = "468c5369b52ca45de3c7e54a3d2ddae352495851";
        String newCommit = "a0b87bf10a9a520b49748c619c868caed8d7a109";

        // THEN:
        new ProjectHarness(project, Verbosity.VERBOSE).compareTwoCommits(
                oldCommit,
                newCommit,
                new GitPathComparer(project.loadRepo(), oldCommit, newCommit),
                new DiagnosticPositionMotionComparer(
                        new GitSrcFilePairLoader(project.loadRepo(), oldCommit, newCommit),
                        newCharacterLineTracker()));
    }

    @Test
    public void example_KnownUnmatchedDiagnostics() throws IOException, GitAPIException {
        // GIVEN:
        CorpusProject project = new GuiceProject();

        DiagnosticsFile oldDiagFile = DiagnosticsFile.load(project,
                "/home/monty/IdeaProjects/java-corpus/diagnostics/guice/8 875868e7263491291d4f8bdc1332bfea746ad673");

        DiagnosticsFile newDiagFile = DiagnosticsFile.load(project,
                "/home/monty/IdeaProjects/java-corpus/diagnostics/guice/22 9b371d3663db9db230417f3cc394e72b705d7d7f");

        // WHEN:
        MatchResults results = GitCommitMatcher.compareGit(project, oldDiagFile, newDiagFile)
                .trackPosition(first(newTokenizedLineTracker(), newIJMStartPosTracker()))
                .match();

        // THEN:
        Assert.assertEquals(6, results.getUnmatchedOldDiagnostics().size());
        Assert.assertEquals(68, results.getUnmatchedNewDiagnostics().size());
    }

    @Test
    public void example_serialiseSpecificCommit() throws IOException {
        CorpusProject project = new HazelcastProject();
        String oldCommit = "ad7bb8210bf4812f48fa630bad924ef07f90e596";

        new ProjectHarness(project, Verbosity.VERBOSE)
                .serialiseCommit(GitUtils.parseCommit(project.loadRepo(), oldCommit),
                        Paths.get("/home/monty/IdeaProjects/java-corpus/diagnostics/hazelcast500/21 ad7bb8210bf4812f48fa630bad924ef07f90e596.skip2"));
    }

    @Test
    public void IJMTest() {
        TestUtils.DiagnosticsPair pair = TestUtils.compareDiagnostics(
                "breaking_changes/sub_expr_changes_old.java",
                "breaking_changes/sub_expr_changes_new.java");

        BugComparer comparer = new DiagnosticPositionMotionComparer(
                new TestSrcFilePairLoader(),
                first(newTokenizedLineTracker(), newIJMStartPosTracker()));

        new DiagnosticsMatcher(
                pair.oldDiagnostics,
                pair.newDiagnostics,
                comparer,
                (oldPath, newPath) -> true).writeToStdout();
    }

    @Test
    public void compareTokenizedWithTokenizedAndIJM() throws Exception {
        // GIVEN:
        CorpusProject project = new GuiceProject();
        String diagFolders = "/home/monty/IdeaProjects/java-corpus/diagnostics/guice50";
//        IntRanges validSeqFiles = IntRanges.include(0, 131).excludeRange(100, 109).exclude(97, 117, 119, 122, 127);

        BugComparerExperiment.forProject(project)
                .withData(LiveDatasetFilePairLoader.allFiles(diagFolders))
//                .withData(LiveDatasetFilePairLoader.specificPairs(diagFolders, 0, 1))
                .comparePaths(withGit(project, GitPathComparer::new))
                .loadDiags(withGit(project, GitSrcFilePairLoader::new))
                .makeBugComparer1(any(newTokenizedLineTracker(), newIJMPosTracker()))
                .makeBugComparer2(any(newTokenizedLineTracker(), newIJMStartAndEndTracker()))
                .findMissedTrackings(MissedLikelihoodCalculatorFactory.diagLineSrcOverlap())
                .trials(9)
                .run("/home/monty/IdeaProjects/java-corpus/comparisons/guice");
    }


    @Test
    public void compareSinglePair() throws IOException, GitAPIException {
        CorpusProject project = new GuiceProject();

        DiagnosticsFile oldFile = DiagnosticsFile.load(project,
                "/home/monty/IdeaProjects/java-corpus/diagnostics/guice50/old");

        DiagnosticsFile newFile = DiagnosticsFile.load(project,
                "/home/monty/IdeaProjects/java-corpus/diagnostics/guice50/new");

        MatchResults results = GitCommitMatcher.compareGit(project, oldFile, newFile)
                .trackPosition(any(newTokenizedLineTracker(), newIJMPosTracker()))
                .match();

        System.out.println(results);
    }

    @Test
    public void trackProject() throws IOException {
        CorpusProject project = new JSoupProject();
        Path output = ProjectFiles.get("java-corpus/comparison_data/jsoup");
        List<GrainDiagFile> grainFiles = GrainDiagFile.loadSortedFiles(
                project, ProjectFiles.get("java-corpus/diagnostics/jsoup_full"));

        MultiGrainDiagFileComparer.compareFiles(project, output, grainFiles);
    }
}
