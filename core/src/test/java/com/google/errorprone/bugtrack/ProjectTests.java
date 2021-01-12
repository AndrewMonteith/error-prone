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
import java.util.Collection;

@RunWith(JUnit4.class)
public class ProjectTests {
    @Test
    public void canScanJSoup() throws IOException {
        // GIVEN:
        CorpusProject project = new JSoupProject();
        String commitHash = "79496d8d047f2d0774e0ad9d8169a021cb828fab";

        // WHEN:
        Collection<Diagnostic<? extends JavaFileObject>> diagnostics =
                new ProjectHarness(project, true).collectDiagnostics(commitHash);

        // THEN:
        Assert.assertTrue(diagnostics.size() > 0);
    }

    @Test
    public void canScanRestAssured() throws IOException {
        // GIVEN:
        CorpusProject project = new RestAssuredProject();
        String commitHash = "9eac8ca6c1d97f8d5a910e553522bdb40752364a";

        // WHEN:
        Collection<Diagnostic<? extends JavaFileObject>> diagnostics =
                new ProjectHarness(project, true).collectDiagnostics(commitHash);

        // THEN:
        Assert.assertTrue(diagnostics.size() > 0);
    }

    @Test
    public void canScanDubbo() throws IOException {
        // GIVEN:
        CorpusProject project = new DubboProject();
        String commitHash = "47fa631c091a94b427881ce3371555fac8c1226f";

        // WHEN:
        Collection<Diagnostic<? extends JavaFileObject>> diagnostics =
                new ProjectHarness(project, true).collectDiagnostics(commitHash);

        // THEN:
        Assert.assertTrue(diagnostics.size() > 0);
    }

    @Test
    public void canScanMetrics() throws IOException {
        // GIVEN:
        CorpusProject project = new DubboProject();
        String commitHash = "e7a0370621aead1b9069e88a994af4b5e8bca25a";

        // WHEN:
        Collection<Diagnostic<? extends JavaFileObject>> diagnostics =
                new ProjectHarness(project, true).collectDiagnostics(commitHash);

        // THEN:
        Assert.assertTrue(diagnostics.size() > 0);
    }

    @Test
    public void canScanJUnit() throws IOException {
        // GIVEN:
        CorpusProject project = new JUnitProject();
        String commitHash = "7a098547474fb11c91262476a172f994e8051ada";
        // Note 9b061ea8c96fa6cba0ac9d7cfd5e8ffbd030b34a does not compile

        // WHEN:
        Collection<Diagnostic<? extends JavaFileObject>> diagnostics =
                new ProjectHarness(project, true).collectDiagnostics(commitHash);

        // THEN:
        Assert.assertTrue(diagnostics.size() > 0);
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
