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

package com.google.errorprone.bugtrack.harness.scanning;

import com.google.common.base.Joiner;
import com.google.common.collect.Iterables;
import com.google.errorprone.bugtrack.CommitRange;
import com.google.errorprone.bugtrack.utils.GitUtils;
import com.google.errorprone.bugtrack.projects.CorpusProject;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static com.google.errorprone.bugtrack.projects.ShouldScanUtils.isJavaFile;

@RunWith(JUnit4.class)
public class GradleProjectScannerTests {

    @Before
    public void setUp() throws GitAPIException {
        new Git(GRADLE_TEST_PROJECT.loadRepo()).checkout().setName("master").call();
    }

    // Duplication from MavenProjectScannerTests... Acceptable for now.
    private static List<Collection<DiagnosticsScan>> collectScans(CommitRange commitRange) throws IOException, InterruptedException, GitAPIException {
        ProjectScanner walker = new GradleProjectScanner();
        walker.cleanProject(GRADLE_TEST_PROJECT.getRoot().toFile());

        Repository testRepo = GRADLE_TEST_PROJECT.loadRepo();

        List<Collection<DiagnosticsScan>> scans = new ArrayList<>();
        for (RevCommit commit : GitUtils.expandCommitRange(testRepo, commitRange)) {
            new Git(testRepo).checkout().setName(commit.getName()).call();
            scans.add(walker.getScans(GRADLE_TEST_PROJECT));
        }

        return scans;
    }

    private static boolean containsFile(DiagnosticsScan scan, String fileName) {
        return scan.files.stream().anyMatch(file -> file.toString().contains(fileName));
    }

    private static void assertIsFiles(DiagnosticsScan scan, String... files) {
        Assert.assertEquals(files.length, scan.files.size());

        for (String file : files) {
            Assert.assertTrue("did not contain file " + file + " in " +
                    Joiner.on(',').join(scan.files), containsFile(scan, file));
        }
    }

    @Test
    public void canTrackFilesEnteringProject() throws InterruptedException, GitAPIException, IOException {
        // GIVEN:
        CommitRange range = new CommitRange("f7219b3c63679326d78f89258e9a2b7aac95385a", "450689a5eb2f880d87a3a6ba96de96177e9ad1a2");

        // WHEN:
        List<Collection<DiagnosticsScan>> scans = collectScans(range);

        // EXPECT:
        Assert.assertEquals(2, scans.size());

        Collection<DiagnosticsScan> firstScans = scans.get(0);
        Assert.assertEquals(2, firstScans.size());

        DiagnosticsScan firstDefaultCompile = Iterables.find(firstScans, scan -> scan.name.equals(":compileJava"));
        assertIsFiles(firstDefaultCompile, "App.java");

        DiagnosticsScan firstTestCompile = Iterables.find(firstScans, scan -> scan.name.equals(":compileTestJava"));
        assertIsFiles(firstTestCompile, "AppTest.java");

        Collection<DiagnosticsScan> secondScans = scans.get(1);
        Assert.assertEquals(2, secondScans.size());

        DiagnosticsScan secondDefaultCompile = Iterables.find(secondScans, scan -> scan.name.equals(":compileJava"));
        assertIsFiles(secondDefaultCompile, "App.java", "File1.java", "File2.java", "File3.java");

        DiagnosticsScan secondTestCompile = Iterables.find(secondScans, scan -> scan.name.equals(":compileTestJava"));
        assertIsFiles(secondTestCompile, "AppTest.java", "AppTest2.java", "AppTest3.java");
    }

    @Test
    public void canTrackFilesLeaving() throws InterruptedException, GitAPIException, IOException {
        // GIVEN:
        CommitRange range = new CommitRange("450689a5eb2f880d87a3a6ba96de96177e9ad1a2", "6c5d496992a2544f44b64cc421f2a43c5b436a8e");

        // WHEN:
        List<Collection<DiagnosticsScan>> scans = collectScans(range);

        // EXPECT:
        Assert.assertEquals(2, scans.size());

        Collection<DiagnosticsScan> firstScans = scans.get(0);
        Assert.assertEquals(2, firstScans.size());

        DiagnosticsScan firstDefaultCompile = Iterables.find(firstScans, scan -> scan.name.equals(":compileJava"));
        assertIsFiles(firstDefaultCompile, "App.java", "File1.java", "File2.java", "File3.java");

        DiagnosticsScan firstTestCompile = Iterables.find(firstScans, scan -> scan.name.equals(":compileTestJava"));
        assertIsFiles(firstTestCompile, "AppTest.java", "AppTest2.java", "AppTest3.java");

        Collection<DiagnosticsScan> secondScans = scans.get(1);
        Assert.assertEquals(2, secondScans.size());

        DiagnosticsScan secondDefaultCompile = Iterables.find(secondScans, scan -> scan.name.equals(":compileJava"));
        assertIsFiles(secondDefaultCompile, "App.java", "File1.java", "File3.java");

        DiagnosticsScan secondTestCompile = Iterables.find(secondScans, scan -> scan.name.equals(":compileTestJava"));
        assertIsFiles(secondTestCompile, "AppTest.java", "AppTest2.java");
    }


    private static final CorpusProject GRADLE_TEST_PROJECT = new CorpusProject() {
        @Override
        public Path getRoot() {
            return Paths.get("/home/monty/IdeaProjects/error-prone/core/src/test/java/com/google/errorprone/bugtrack/testdata/gradle_test_repo/");
        }

        @Override
        public boolean shouldScanFile(Path file) {
            return isJavaFile(file);
        }

        @Override
        public BuildSystem getBuildSystem() {
            return BuildSystem.Maven;
        }
    };
}
