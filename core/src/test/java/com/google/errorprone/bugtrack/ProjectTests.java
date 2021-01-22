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
import com.google.errorprone.bugtrack.harness.ProjectHarness;
import com.google.errorprone.bugtrack.projects.*;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@RunWith(JUnit4.class)
public class ProjectTests {

    private void assertFindsDiagnostics(CorpusProject project, String commitHash) throws IOException {
        // WHEN:
        GitUtils.checkoutMaster(project.loadRepo());
        Collection<Diagnostic<? extends JavaFileObject>> diagnostics =
                new ProjectHarness(project, true).collectDiagnostics(commitHash);

        // THEN:
        Assert.assertTrue(diagnostics.size() > 0);
    }

    @Test
    public void canScanJSoup() throws IOException {
        assertFindsDiagnostics(new JSoupProject(),
                               "79496d8d047f2d0774e0ad9d8169a021cb828fab");
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
                               "a5a4960859d0d15bfe677be86291cd1e59047436");
    }

    @Test
    public void findInterestingCommitPairs() throws IOException, GitAPIException {
        // GIVEN:
        CorpusProject springFrameworkProject = new SpringFrameworkProject();
        CommitRange range = new CommitRange("0f1f95e0909c5d32bbc9305ae85c57312a491058", "a5a4960859d0d15bfe677be86291cd1e59047436");

        // WHEN:
        new ProjectHarness(springFrameworkProject).findInterestingPairs(range);
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
    public void example_DetectingNewBug() throws IOException, GitAPIException {
        // GIVEN:
        CorpusProject project = new JSoupProject();

        String oldCommit = "468c5369b52ca45de3c7e54a3d2ddae352495851";
        String newCommit = "a0b87bf10a9a520b49748c619c868caed8d7a109";

        BugComparer comparer = new LineMotionComparer(project.loadRepo(), oldCommit, newCommit);

        // THEN:
        new ProjectHarness(project).compareTwoCommits(oldCommit, newCommit, comparer);
    }

    @Test
    public void walkAProject() throws IOException, GitAPIException {
        // GIVEN:
        CorpusProject project = new JUnitProject();
        CommitRange range = new CommitRange("0900bc02145d1793149433332d41181b9bef84fe",
                "50a285d3ce69b4556ac46d8633f6beb4527b4679");

        // THEN:
        new ProjectHarness(project, false).walkCommitRange(range);
    }
}
