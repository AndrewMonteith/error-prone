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
import com.google.errorprone.bugtrack.harness.DiagnosticsDistribution;
import com.google.errorprone.bugtrack.harness.LinesChangedCommitFilter;
import com.google.errorprone.bugtrack.harness.ProjectHarness;
import com.google.errorprone.bugtrack.projects.*;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
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
import java.util.function.Predicate;

@RunWith(JUnit4.class)
public class ProjectTests {

    private void assertFindsDiagnostics(CorpusProject project, String commitHash) throws IOException {
        // WHEN:
        GitUtils.checkoutMaster(project.loadRepo());
        Collection<Diagnostic<? extends JavaFileObject>> diagnostics =
                new ProjectHarness(project, true).collectDiagnostics(commitHash);

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
        // GIVEN:
        CorpusProject jsoup = new JSoupProject();
        String commitHash = "468c5369b52ca45de3c7e54a3d2ddae352495851";
        Path output = Paths.get("/home/monty/IdeaProjects/java-corpus/jsoup_output");

        // WHEN:
        new ProjectHarness(jsoup, true).serialiseCommit(commitHash, output.toString());

        // THEN:
        Assert.assertEquals(376, DatasetDiagnosticsFile.loadFromFile(output).diagnostics.size());
    }

    @Test
    public void serialiseDiagnostics() throws IOException, GitAPIException {
        CorpusProject project  = new GuiceProject();
        CommitRange range = new CommitRange("0367f870f7d412918dab2c596bb6b0ac3f4e93ca", "1a299822f02642b5cdc6606430266987d0bb4b24");

        new ProjectHarness(project).serialiseCommits(range,
                new LinesChangedCommitFilter(new Git(project.loadRepo()), 50),
                "/home/monty/IdeaProjects/java-corpus/diagnostics/guice");

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
        assertFindsDiagnostics(new DubboProject(),
                               "e7a0370621aead1b9069e88a994af4b5e8bca25a");
    }

    @Test
    public void canScanJUnit() throws IOException {
        // Note 9b061ea8c96fa6cba0ac9d7cfd5e8ffbd030b34a does not compile
        assertFindsDiagnostics(new JUnitProject(),
                               "7a098547474fb11c91262476a172f994e8051ada");
    }

    @Test
    public void canScanSpringFramework() throws IOException {
        assertFindsDiagnostics(new SpringFrameworkProject(),
                               "1cb9f2c7b2a25daae014349ba3df7823b3584171");
    }

    @Test
    public void detectsChangesInDiagnosticsInMavenProject() throws IOException {
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
        new ProjectHarness(project, true).forEachCommitIdWithDiagnostics(
                commits, (commit, diagnostics) -> numberOfDiagnostics.add(diagnostics.size()));

        // THEN:
        Assert.assertEquals(4, numberOfDiagnostics.size());
        Assert.assertEquals((int)numberOfDiagnostics.get(0), 376);
        Assert.assertEquals((int)numberOfDiagnostics.get(1), 376);
        Assert.assertEquals((int)numberOfDiagnostics.get(2), 378);
        Assert.assertEquals((int)numberOfDiagnostics.get(3), 378);
    }


    @Test
    public void canWalkCommits() throws IOException, GitAPIException {
        // GIVEN:
        CorpusProject project = new JSoupProject();
        CommitRange range = new CommitRange("3c37bffe", "690d6019");
        // THEN:
        new ProjectHarness(project, true).walkCommitRange(range);
    }

    @Test
    public void example_DetectingNewBugWithScanning() throws IOException, GitAPIException {
        // GIVEN:
        CorpusProject project = new JSoupProject();

        String oldCommit = "468c5369b52ca45de3c7e54a3d2ddae352495851";
        String newCommit = "a0b87bf10a9a520b49748c619c868caed8d7a109";

        BugComparer comparer = new LineMotionComparer(project.loadRepo(), oldCommit, newCommit);

        // THEN:
        new ProjectHarness(project, true).compareTwoCommits(oldCommit, newCommit, comparer);
    }

    @Test
    public void example_DetectingNewBugWithLogs() throws IOException {

    }

    @Test
    public void example_CompareLogFiles() throws IOException, GitAPIException {
        // GIVEN:
        CorpusProject project = new GuiceProject();

        DatasetDiagnosticsFile oldDiagnostics = DatasetDiagnosticsFile.loadFromFile(
                Paths.get("/home/monty/IdeaProjects/java-corpus/diagnostics/guice/8 875868e7263491291d4f8bdc1332bfea746ad673"));

        DatasetDiagnosticsFile newDiagnostics = DatasetDiagnosticsFile.loadFromFile(
                Paths.get("/home/monty/IdeaProjects/java-corpus/diagnostics/guice/22 9b371d3663db9db230417f3cc394e72b705d7d7f"));


        BugComparer comparer = new LineMotionComparer(project.loadRepo(), oldDiagnostics.commitId, newDiagnostics.commitId);

        /*
            Between commits: 875868e7263491291d4f8bdc1332bfea746ad673 and 9b371d3663db9db230417f3cc394e72b705d7d7f in Guice
              the following commits are not tracked properly:

            OLD:
              /home/monty/IdeaProjects/java-corpus/guice/core/src/com/google/inject/util/Modules.java 240 38
              [AndroidJdkLibsChecker] java.util.Map#computeIfAbsent(K,java.util.function.Function<? super K,? extends V>) is not available in java.util.Map
                   (see https://errorprone.info/bugpattern/AndroidJdkLibsChecker)
            NEW:
              /home/monty/IdeaProjects/java-corpus/guice/core/src/com/google/inject/util/Modules.java 281 19
              [AndroidJdkLibsChecker] java.util.Map#computeIfAbsent(K,java.util.function.Function<? super K,? extends V>) is not available in java.util.Map
                    (see https://errorprone.info/bugpattern/AndroidJdkLibsChecker)

            This is 875868e7263491291d4f8bdc1332bfea746ad673:Modules.java is
                240: scopeInstancesInUse.computeIfAbsent(scope, k -> Lists.newArrayList());
            And 9b371d3663db9db230417f3cc394e72b705d7d7f:Modules.java is
                280: scopeInstancesInUse
                281:     .computeIfAbsent(scope, k -> Lists.newArrayList())

            In reality git diff information works on a line by line basis. So since the expression splits apart
              onto two lines then that's why we cannot track.
         */

        // THEN:
        new ProjectHarness(project).compareDiagnosticsFile(oldDiagnostics, newDiagnostics, comparer);
    }
}
