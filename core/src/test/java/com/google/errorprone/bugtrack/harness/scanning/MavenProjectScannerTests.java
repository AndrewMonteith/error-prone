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
import com.google.errorprone.bugtrack.harness.utils.CommitDAGPathFinders;
import com.google.errorprone.bugtrack.projects.CorpusProject;
import com.google.errorprone.bugtrack.utils.ProjectFiles;
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static com.google.errorprone.bugtrack.projects.ShouldScanUtils.isJavaFile;

@RunWith(JUnit4.class)
public class MavenProjectScannerTests {

  private static final CorpusProject MAVEN_TEST_PROJECT =
      new CorpusProject() {
        @Override
        public Path getRoot() {
          return ProjectFiles.get(
              "error-prone/core/src/test/java/com/google/errorprone/bugtrack/testdata/maven_test_repo/");
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

  private static List<Collection<DiagnosticsScan>> collectScans(CommitRange commitRange)
      throws IOException, InterruptedException, GitAPIException {
    ProjectScanner walker = new MavenProjectScanner();
    walker.cleanProject(MAVEN_TEST_PROJECT.getRoot());

    Repository testRepo = MAVEN_TEST_PROJECT.loadRepo();

    List<Collection<DiagnosticsScan>> scans = new ArrayList<>();
    for (RevCommit commit : CommitDAGPathFinders.in(testRepo, commitRange).dfs()) {
      new Git(testRepo).checkout().setName(commit.getName()).call();
      scans.add(walker.getScans(MAVEN_TEST_PROJECT));
    }

    return scans;
  }

  private static boolean containsFile(DiagnosticsScan scan, String fileName) {
    return scan.files.stream().anyMatch(file -> file.toString().contains(fileName));
  }

  private static void assertIsFiles(DiagnosticsScan scan, String... files) {
    Assert.assertEquals(files.length, scan.files.size());

    for (String file : files) {
      Assert.assertTrue(
          "did not contain file " + file + " in " + Joiner.on(',').join(scan.files),
          containsFile(scan, file));
    }
  }

  @Before
  public void setUp() throws GitAPIException {
    new Git(MAVEN_TEST_PROJECT.loadRepo()).checkout().setName("master").call();
  }

  @Test
  public void catchesFilesBeingAdded() throws IOException, InterruptedException, GitAPIException {
    // GIVEN:
    CommitRange range =
        new CommitRange(
            "0c9d2b9520afab7501402ca7924a22c391ad9d8a", "05fe468e3e0c74a96057919c9262b0c8ccac2b5a");

    // WHEN:
    List<Collection<DiagnosticsScan>> scans = collectScans(range);

    // THEN:
    Assert.assertEquals(2, scans.size());

    Collection<DiagnosticsScan> firstScans = scans.get(0);
    Assert.assertEquals(2, firstScans.size());

    DiagnosticsScan firstDefaultCompile =
        Iterables.find(firstScans, scan -> scan.name.equals("default-compile my-app"));
    assertIsFiles(firstDefaultCompile, "App.java");

    DiagnosticsScan firstTestCompile =
        Iterables.find(firstScans, scan -> scan.name.equals("default-testCompile my-app"));
    assertIsFiles(firstTestCompile, "AppTest.java");

    Collection<DiagnosticsScan> secondScans = scans.get(1);
    Assert.assertEquals(2, secondScans.size());

    DiagnosticsScan secondDefaultCompile =
        Iterables.find(secondScans, scan -> scan.name.equals("default-compile my-app"));
    assertIsFiles(secondDefaultCompile, "App.java", "Test1.java", "Test2.java");

    DiagnosticsScan secondTestCompile =
        Iterables.find(secondScans, scan -> scan.name.equals("default-testCompile my-app"));
    assertIsFiles(secondTestCompile, "AppTest.java");
  }

  @Test
  public void canDetectRenamingFiles() throws InterruptedException, GitAPIException, IOException {
    // GIVEN:
    CommitRange range =
        new CommitRange(
            "05fe468e3e0c74a96057919c9262b0c8ccac2b5a", "816b2a9e0b350a07e4bedf85c638acec57ed0dee");

    // WHEN:
    List<Collection<DiagnosticsScan>> scans = collectScans(range);

    // THEN:
    Assert.assertEquals(2, scans.size());

    Collection<DiagnosticsScan> firstScans = scans.get(0);
    Assert.assertEquals(2, firstScans.size());

    DiagnosticsScan firstDefaultCompile =
        Iterables.find(firstScans, scan -> scan.name.equals("default-compile my-app"));
    assertIsFiles(firstDefaultCompile, "App.java", "Test1.java", "Test2.java");

    DiagnosticsScan firstTestCompile =
        Iterables.find(firstScans, scan -> scan.name.equals("default-testCompile my-app"));
    assertIsFiles(firstTestCompile, "AppTest.java");

    Collection<DiagnosticsScan> secondScans = scans.get(1);
    Assert.assertEquals(2, secondScans.size());

    DiagnosticsScan secondDefaultCompile =
        Iterables.find(secondScans, scan -> scan.name.equals("default-compile my-app"));
    assertIsFiles(secondDefaultCompile, "App.java", "Test1.java", "Test3.java", "Test4.java");

    DiagnosticsScan secondTestCompile =
        Iterables.find(secondScans, scan -> scan.name.equals("default-testCompile my-app"));
    assertIsFiles(secondTestCompile, "AppTest.java");
  }

  @Test
  public void canDetectFilesChangesInMultipleTargets()
      throws InterruptedException, GitAPIException, IOException {
    // GIVEN:
    CommitRange range =
        new CommitRange(
            "816b2a9e0b350a07e4bedf85c638acec57ed0dee", "d2b47d6405825929063ea852a3256842a227b29d");

    // WHEN:
    List<Collection<DiagnosticsScan>> scans = collectScans(range);

    // THEN:
    Assert.assertEquals(2, scans.size());

    Collection<DiagnosticsScan> firstScans = scans.get(0);
    Assert.assertEquals(2, firstScans.size());

    DiagnosticsScan firstDefaultCompile =
        Iterables.find(firstScans, scan -> scan.name.equals("default-compile my-app"));
    assertIsFiles(firstDefaultCompile, "App.java", "Test1.java", "Test3.java", "Test4.java");

    DiagnosticsScan firstTestCompile =
        Iterables.find(firstScans, scan -> scan.name.equals("default-testCompile my-app"));
    assertIsFiles(firstTestCompile, "AppTest.java");

    Collection<DiagnosticsScan> secondScans = scans.get(1);
    Assert.assertEquals(2, secondScans.size());

    DiagnosticsScan secondDefaultCompile =
        Iterables.find(secondScans, scan -> scan.name.equals("default-compile my-app"));
    assertIsFiles(secondDefaultCompile, "App.java", "Test1.java", "Test3.java");

    DiagnosticsScan secondTestCompile =
        Iterables.find(secondScans, scan -> scan.name.equals("default-testCompile my-app"));
    assertIsFiles(secondTestCompile, "AppTest.java", "AppTest2.java");
  }
}
