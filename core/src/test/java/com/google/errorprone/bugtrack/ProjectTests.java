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
import com.google.errorprone.bugtrack.harness.LinesChangedCommitFilter;
import com.google.errorprone.bugtrack.harness.ProjectHarness;
import com.google.errorprone.bugtrack.harness.Verbosity;
import com.google.errorprone.bugtrack.harness.evaluating.BugComparerExperiment;
import com.google.errorprone.bugtrack.harness.evaluating.IntRanges;
import com.google.errorprone.bugtrack.harness.evaluating.LiveDatasetFilePairLoader;
import com.google.errorprone.bugtrack.harness.evaluating.MissedLikelihoodCalculatorFactory;
import com.google.errorprone.bugtrack.harness.matching.DiagnosticsMatcher;
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
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import static com.google.errorprone.bugtrack.harness.evaluating.BugComparerExperiment.withGit;
import static com.google.errorprone.bugtrack.motion.trackers.DPTrackerConstructorFactory.*;
//mvn -Dtest=ProjectTests#canScanJSoup -DfailIfNoTests=false test
    
@RunWith(JUnit4.class)
public class ProjectTests {
    private void assertFindsDiagnostics(CorpusProject project, String commitHash) throws IOException {
        // WHEN:
        GitUtils.checkoutMaster(project.loadRepo());

        Collection<Diagnostic<? extends JavaFileObject>> diagnostics =
                DiagnosticsCollector.collectEPDiagnostics(project, commitHash, Verbosity.VERBOSE);

//        diagnostics.forEach(d -> {
//            System.out.println(d);
//            System.out.println(d.getStartPosition() + " " + d.getEndPosition());
//        });

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
        // GIVEN:
//        CorpusProject jsoup = new GuiceProject();
////        String oldCommit = "468c5369b52ca45de3c7e54a3d2ddae352495851";
////        String newCommit = "a0b87bf10a9a520b49748c619c868caed8d7a109";
//        String commitHash = "dd873be40013bffbe07552367326bd2b60eaa807";
//        String newCommitHash = "37255a24d9bfc37bf8b76cb594d05e3203c984e0";
//        Path output = Paths.get("/home/monty/IdeaProjects/java-corpus/guice_85");
//
//        // WHEN:
//        new ProjectHarness(jsoup).serialiseCommit(commitHash, output.toString());
//
//        // THEN:
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
    public void serialiseDiagnostics() throws IOException, GitAPIException {
        CorpusProject project = new ZXingProject();
        CommitRange range = new CommitRange("f5b5c3a97ed1d3be6156cb4d27f0428b95ec4275", "44267115986e7fdcb2a9629fd7986f65ab9a2670");

        new ProjectHarness(project).serialiseCommits(range,
                new LinesChangedCommitFilter(new Git(project.loadRepo()), 10),
                Paths.get("/home/monty/IdeaProjects/java-corpus/diagnostics/zxing"));

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

        BugComparer bugComparer = new DiagnosticPositionMotionComparer(
                new GitSrcFilePairLoader(project.loadRepo(), oldDiagFile.commitId, newDiagFile.commitId),
                first(newTokenizedLineTracker(), newIJMStartPosTracker()));

        // WHEN:
        MatchResults results = DiagnosticsMatcher.fromFiles(
                project, oldDiagFile, newDiagFile, bugComparer).getResults();

        // THEN:
        Assert.assertEquals(6, results.getUnmatchedOldDiagnostics().size());
        Assert.assertEquals(68, results.getUnmatchedNewDiagnostics().size());
    }

    @Test
    public void foo() throws IOException {
        // GIVEN:
        CorpusProject project = new ZXingProject();
        String oldCommit = "f5b5c3a97ed1d3be6156cb4d27f0428b95ec4275";
        String newCommit = "a0b87bf10a9a520b49748c619c868caed8d7a109";

        new ProjectHarness(project, Verbosity.VERBOSE)
                .serialiseCommit(GitUtils.parseCommit(project.loadRepo(), oldCommit),
                        Paths.get("/home/monty/IdeaProjects/java-corpus/diagnostics/zxing_old"));

//        DatasetDiagnosticsFile oldFile = DatasetDiagnosticsFile.loadFromFile(
//                Paths.get("/home/monty/IdeaProjects/java-corpus/diagnostics/jsoup/0 468c5369b52ca45de3c7e54a3d2ddae352495851"));
//
//        DatasetDiagnosticsFile newFile = DatasetDiagnosticsFile.loadFromFile(
//                Paths.get("/home/monty/IdeaProjects/java-corpus/diagnostics/jsoup/1 a0b87bf10a9a520b49748c619c868caed8d7a109"));
//
//        System.out.println(new DiagnosticsDistribution(oldFile.diagnostics));
    }

    @Test
    public void example_CompareLogFiles() throws IOException, GitAPIException {
        // GIVEN:
        CorpusProject project = new GuiceProject();

        DiagnosticsFile oldFile = DiagnosticsFile.load(project,
                "/home/monty/IdeaProjects/java-corpus/diagnostics/guice/8 875868e7263491291d4f8bdc1332bfea746ad673");

        DiagnosticsFile newFile = DiagnosticsFile.load(project,
                "/home/monty/IdeaProjects/java-corpus/diagnostics/guice/82 b7cadc1cfa0623ad377c274eb8db278e3e9a7054");


        BugComparer comparer = new DiagnosticPositionMotionComparer(
                new GitSrcFilePairLoader(project.loadRepo(), oldFile.commitId, newFile.commitId),
                first(newTokenizedLineTracker(), newIJMStartPosTracker()));

        // THEN:
        DiagnosticsMatcher.fromFiles(project, oldFile, newFile, comparer).writeToStdout();
//        new DiagnosticsMatcher(oldDiagnostics.diagnostics, newDiagnostics.diagnostics, comparer).writeToFile(
//                Paths.get("/home/monty/IdeaProjects/java-corpus/diagnostics"));
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
        CorpusProject project = new MyBatis3Project();
        String diagFolders = "/home/monty/IdeaProjects/java-corpus/diagnostics/mybatis3";
        IntRanges validSeqFiles = IntRanges.include(0, 131).excludeRange(100, 109).exclude(97, 117, 119, 122, 127);

        BugComparerExperiment.forProject(project)
                .withData(LiveDatasetFilePairLoader.inSeqNumRange(diagFolders, validSeqFiles))
                .comparePaths(withGit(project, GitPathComparer::new))
                .loadDiags(withGit(project, GitSrcFilePairLoader::new))
                .makeBugComparer1(any(newTokenizedLineTracker(), newIJMStartPosTracker()))
                .makeBugComparer2(any(newTokenizedLineTracker(), newIJMStartAndEndTracker()))
                .findMissedTrackings(MissedLikelihoodCalculatorFactory.diagLineSrcOverlap())
                .trials(25)
                .run("/home/monty/IdeaProjects/java-corpus/comparisons/mybatis3");
    }


    @Test
    public void compareSinglePair() throws IOException, GitAPIException {
        CorpusProject project = new JSoupProject();

        DiagnosticsFile oldFile = DiagnosticsFile.load(project,
                "/home/monty/IdeaProjects/java-corpus/diagnostics/jsoup/76 8e432a5f5fde4694834ce23c5ac1503ce8d381bc");

        DiagnosticsFile newFile = DiagnosticsFile.load(project,
                "/home/monty/IdeaProjects/java-corpus/diagnostics/jsoup/78 6e3c98c4a578fb7910241e72a5a33014bb8e85f1");

//        System.out.println(new DiagnosticsDistribution(oldFile.diagnostics));
//        System.out.println(new DiagnosticsDistribution(newFile.diagnostics));

        BugComparer bugComparer = new DiagnosticPositionMotionComparer(
                new GitSrcFilePairLoader(project.loadRepo(), oldFile.commitId, newFile.commitId),
                any(newTokenizedLineTracker(), newIJMStartAndEndTracker()));

        DiagnosticsMatcher matcher = DiagnosticsMatcher.fromFiles(project, oldFile, newFile, bugComparer);

        MatchResults results = matcher.getResults();

//        System.out.println(results);
        System.out.println(results.getMatchedDiagnostics().size());
        System.out.println(results.getUnmatchedOldDiagnostics().size());
        System.out.println(results.getUnmatchedNewDiagnostics().size());
    }

    @Test
    public void trackJSoupDiagnostics() throws IOException, GitAPIException {
        CorpusProject project = new JSoupProject();
        Path diagnosticsFile = ProjectFiles.get("java-corpus/diagnostics/jsoup");

        DiagnosticsFile[] files = new DiagnosticsFile[102];
        for (String f : Objects.requireNonNull(diagnosticsFile.toFile().list())) {
            int seq = DiagnosticsFile.getSequenceNumberFromName(f);
            files[seq] = DiagnosticsFile.load(project, diagnosticsFile.resolve(f));
        }

        for (int seq = 1; seq < files.length; ++seq) {
            int prior = (seq == 78 ? 76 : seq-1);

            DiagnosticsFile before = files[prior];
            DiagnosticsFile current = files[seq];

            {
                BugComparer bugComparer = new DiagnosticPositionMotionComparer(
                        new GitSrcFilePairLoader(project.loadRepo(), before.commitId, current.commitId),
                        any(newTokenizedLineTracker(), newIJMStartAndEndTracker()));

                DiagnosticsMatcher matcher = DiagnosticsMatcher.fromFiles(project, before, current, bugComparer);

                MatchResults results = matcher.getResults();

                results.save(ProjectFiles.get("java-corpus/comparison_data/jsoup").resolve(prior + " -> " + seq));
            }

            System.gc();
        }
    }
}
