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

import com.github.gumtreediff.gen.jdt.AbstractJdtTreeGenerator;
import com.github.gumtreediff.gen.jdt.AbstractJdtVisitor;
import com.github.gumtreediff.tree.TreeContext;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.errorprone.bugtrack.harness.DiagnosticsFile;
import com.google.errorprone.bugtrack.harness.JavaLinesChangedFilter;
import com.google.errorprone.bugtrack.harness.ProjectHarness;
import com.google.errorprone.bugtrack.harness.Verbosity;
import com.google.errorprone.bugtrack.harness.evaluating.GrainDiagFile;
import com.google.errorprone.bugtrack.harness.evaluating.MultiGrainDiagFileComparer;
import com.google.errorprone.bugtrack.harness.matching.DiagnosticsMatcher;
import com.google.errorprone.bugtrack.harness.matching.MatchResults;
import com.google.errorprone.bugtrack.harness.scanning.DiagnosticsCollector;
import com.google.errorprone.bugtrack.harness.scanning.DiagnosticsScan;
import com.google.errorprone.bugtrack.harness.utils.CommitDAGPathFinders;
import com.google.errorprone.bugtrack.motion.SrcFile;
import com.google.errorprone.bugtrack.motion.trackers.BetterJdtVisitor;
import com.google.errorprone.bugtrack.motion.trackers.DiagnosticPredicates;
import com.google.errorprone.bugtrack.projects.*;
import com.google.errorprone.bugtrack.utils.GitUtils;
import com.google.errorprone.bugtrack.utils.ProjectFiles;
import com.google.googlejavaformat.java.Formatter;
import com.google.googlejavaformat.java.FormatterException;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
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
import java.util.Map;

import static com.google.errorprone.bugtrack.BugComparers.*;
import static com.google.errorprone.bugtrack.motion.trackers.DiagnosticPositionTrackers.*;

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
    assertFindsDiagnostics(new JSoupProject(), "91ca25b341bc5ad1c364b8e7389287c45ca9df2c");
  }

  @Test
  public void canScanGuice() throws IOException {
    assertFindsDiagnostics(new GuiceProject(), "dafa4b0bec4e7ec5e1df75e3fb9a2fdf4920921a");
  }

  @Test
  public void canScanMyBatis3() throws IOException {
    assertFindsDiagnostics(new MyBatis3Project(), "f6788c7a100eb6cb53b0b7401a31c469198ec017");
  }

  @Test
  public void canSerialiseDiagnostics2() throws IOException {
    assertFindsDiagnostics(new GuiceProject(), "dafa4b0bec4e7ec5e1df75e3fb9a2fdf4920921a");
  }

  @Test
  public void canSerialiseDiagnostics() throws IOException {
    // Will only pass on defaultChecks

    // GIVEN:
    CorpusProject jsoup = new JSoupProject();
    RevCommit commit =
        GitUtils.parseCommit(jsoup.loadRepo(), "468c5369b52ca45de3c7e54a3d2ddae352495851");
    Path output = Paths.get("/home/monty/IdeaProjects/java-corpus/jsoup_output");

    // WHEN:
    new ProjectHarness(jsoup, Verbosity.VERBOSE).serialiseCommit(commit, output);

    // THEN:
    Assert.assertEquals(376, DiagnosticsFile.load(jsoup, output).diagnostics.size());
  }

  @Test
  public void serialiseDiagnostics() throws IOException, GitAPIException, InterruptedException {
    CorpusProject project = new JSoupProject();
    CommitRange range =
        new CommitRange(
            "f1110a9021c2caa28cbe3177c0c3a0f5ae326eb4", "ae9a18c9e1382b5d8bad14d09279eda725490c25");

    new ProjectHarness(project)
        .serialiseCommits(
            range,
            new JavaLinesChangedFilter(new Git(project.loadRepo()), 25),
            Paths.get("/home/monty/IdeaProjects/java-corpus/diagnostics/jsoup25"));
  }

  @Test
  public void canScanRestAssured() throws IOException {
    // GIVEN:
    assertFindsDiagnostics(new RestAssuredProject(), "9eac8ca6c1d97f8d5a910e553522bdb40752364a");
  }

  @Test
  public void canScanDubbo() throws IOException {
    assertFindsDiagnostics(new DubboProject(), "47fa631c091a94b427881ce3371555fac8c1226f");
  }

  @Test
  public void canScanMetrics() throws IOException {
    assertFindsDiagnostics(new MetricsProject(), "e7a0370621aead1b9069e88a994af4b5e8bca25a");
  }

  @Test
  public void canScanJUnit() throws IOException {
    // Note 9b061ea8c96fa6cba0ac9d7cfd5e8ffbd030b34a does not compile
    assertFindsDiagnostics(new JUnitProject(), "138d278012f9d647f983f59c3ede954ea111bdd5");
  }

  @Test
  public void canScanSpringFramework() throws IOException {
    assertFindsDiagnostics(
        new SpringFrameworkProject(), "1cb9f2c7b2a25daae014349ba3df7823b3584171");
  }

  @Test
  public void canScanHibernate() throws IOException {
    assertFindsDiagnostics(new HibernateProject(), "6cead49fec02761cb616fe8ea20134b90115f8b5");
  }

  @Test
  public void canScanOkHttp() throws IOException {
    assertFindsDiagnostics(new OkHttpProject(), "5d72f8980d841173dd0d52ae0422e8ea04ef1e09");
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
    List<String> commits =
        ImmutableList.of(
            "3c37bffed94c19c5f500217eb568bcdf394be64e",
            "afd73606a90909444e1c443b555dae7b71e6a5a0",
            "724b2c5bf576cbd548738756bfe5f7a7b90c6239",
            "690d601950bf44fc84dcc711b2ef265f9542df62");

    // WHEN:
    List<Integer> numberOfDiagnostics = new ArrayList<>();
    new ProjectHarness(project, Verbosity.VERBOSE)
        .forEachCommitIdWithDiagnostics(
            commits, (commit, diagnostics) -> numberOfDiagnostics.add(diagnostics.size()));

    // THEN:
    Assert.assertEquals(4, numberOfDiagnostics.size());
    Assert.assertEquals((int) numberOfDiagnostics.get(0), 376);
    Assert.assertEquals((int) numberOfDiagnostics.get(1), 376);
    Assert.assertEquals((int) numberOfDiagnostics.get(2), 378);
    Assert.assertEquals((int) numberOfDiagnostics.get(3), 378);
  }

  @Test
  public void example_serialiseSpecificCommit() throws IOException {
    CorpusProject project = new HazelcastProject();
    String oldCommit = "ad7bb8210bf4812f48fa630bad924ef07f90e596";

    new ProjectHarness(project, Verbosity.VERBOSE)
        .serialiseCommit(
            GitUtils.parseCommit(project.loadRepo(), oldCommit),
            Paths.get(
                "/home/monty/IdeaProjects/java-corpus/diagnostics/hazelcast500/21 ad7bb8210bf4812f48fa630bad924ef07f90e596.skip2"));
  }

  @Test
  public void compareSinglePair() throws IOException, GitAPIException {
    CorpusProject project = new CheckstyleProject();

    DiagnosticsFile oldFile =
        DiagnosticsFile.load(project, "/home/monty/IdeaProjects/java-corpus/diagnostics/checkstyle/" +
                "105 f4e2b8f58dbb536ad9206828bcf70a81007b45d0.12-25-50");

    DiagnosticsFile newFile =
        DiagnosticsFile.load(project, "/home/monty/IdeaProjects/java-corpus/diagnostics/checkstyle/107 55522d255b3f58990f7df1cecd92d10ed5abc00b.12-25-50");

    BugComparerCtor tracker =
        BugComparers.and(
            matchProblem(),
            conditional(
                DiagnosticPredicates.canTrackIdenticalLocation(),
                matchIdenticalLocation(),
                trackPosition(newIJMStartAndEndTracker())));
    // Warmup
    DiagnosticsMatcher.fromFiles(project, oldFile, newFile, tracker).match();

    TimingInformation timeInformation = new TimingInformation();

    DiagnosticsMatcher.fromFiles(project, oldFile, newFile, tracker, timeInformation).match();

    System.out.println(timeInformation.timeSpentComparingChangedFiles / 1_000_000_000);

   //    }
  }

  @Test
  public void printCommitsScanned() throws IOException, GitAPIException {
    ImmutableList<CorpusProject> projects =
        ImmutableList.of(
            new JSoupProject(),
            new CheckstyleProject(),
            new CoberturaProject(),
            new DubboProject(),
            new GuiceProject(),
            new HazelcastProject(),
            new JRubyProject(),
            new McMMOProject(),
            new MetricsProject(),
            new JUnitProject());

    ImmutableList<List<String>> commitsRanges =
        ImmutableList.of(
            ImmutableList.of(
                "f1110a9021c2caa28cbe3177c0c3a0f5ae326eb4",
                "ae9a18c9e1382b5d8bad14d09279eda725490c25"),
            ImmutableList.of(
                "ec4d06712ab203d31d73c5c6d5c46067f3a6d5b3",
                "f1e8346ef9dc13c9d778bb35a8821d43d409d003"),
            ImmutableList.of(
                "2be4bbb667f41a47143153def1b76880dc85851a",
                "f986347ec66fe9443c6f48d0995c658ed34c1704"),
            ImmutableList.of(
                "1bc1312c300ffa17713a665032d2154b12f91424",
                "0e381aafb244a9af30a99d291143fcb3e19a2ec7"),
            ImmutableList.of(
                "0367f870f7d412918dab2c596bb6b0ac3f4e93ca",
                "1a299822f02642b5cdc6606430266987d0bb4b24"),
            ImmutableList.of(
                "6a5bc11894e312366e82d4c808df31c2d441d0fc",
                "1d86daea33db15b04bc1da84325f2bda17b44cfb"),
            ImmutableList.of(
                "3c4ab50125f9d5e6375e316c6d1b4930eb7c29c7",
                "dbbc69abb42d920d2b93a0e17027b4830370d79a"),
            ImmutableList.of(
                "2e7f56eeb5faaf71d14fe657a35b0b766e2ffe41",
                "a7ded7e98225b565e6f4a800505efc1f2d593e6c"),
            ImmutableList.of(
                "b6d6b748f0fd851d3050f7ba15d8e3c6f3954868",
                "e4d8a84cb826a43f35bdd369cb8f20f77b205679"),
            ImmutableList.of(
                "54b7613484be714a769a8d62f1ac507912e61a01",
                "9ad61c6bf757be8d8968fd5977ab3ae15b0c5aba"));

    ImmutableList<Integer> grainSizes = ImmutableList.of(50, 50, 25, 50, 50, 500, 50, 200, 50, 100);

    for (int i = 0; i < projects.size(); ++i) {
      Repository repo = projects.get(i).loadRepo();
      List<String> commitRange = commitsRanges.get(i);

      CommitRange range = new CommitRange(commitRange.get(0), commitRange.get(1));

      int commits = CommitDAGPathFinders.in(repo, range).dfs().size();

      System.out.println("Total commits  " + commits);
    }
  }

  @Test
  public void trackProject() throws IOException {
    CorpusProject project = new JUnitProject();
    Path output = ProjectFiles.get("java-corpus/comparison_data/junit4");
    List<GrainDiagFile> grainFiles =
        GrainDiagFile.loadSortedFiles(
            project, ProjectFiles.get("java-corpus/diagnostics/junit4_full"));

    MultiGrainDiagFileComparer.compareFiles(project, output, grainFiles, false);
  }

  @Test
  public void scanSpecificFiles() {
    DiagnosticsScan scan =
        new DiagnosticsScan(
            "foo",
            ImmutableList.of(
                new ProjectFile(
                    new HazelcastProject(),
                    "hazelcast/src/main/java/com/hazelcast/cache/impl/DeferredValue.java")),
            Splitter.on(' ')
                .splitToList(
                    "-d /home/monty/IdeaProjects/java-corpus/hazelcast/hazelcast/target/classes -classpath /home/monty/IdeaProjects/java-corpus/hazelcast/hazelcast/target/classes:/home/monty/IdeaProjects/.m2/repository/com/fasterxml/jackson/core/jackson-core/2.9.7/jackson-core-2.9.7.jar:/home/monty/IdeaProjects/.m2/repository/org/snakeyaml/snakeyaml-engine/1.0/snakeyaml-engine-1.0.jar:/home/monty/IdeaProjects/.m2/repository/log4j/log4j/1.2.17/log4j-1.2.17.jar:/home/monty/IdeaProjects/.m2/repository/org/apache/logging/log4j/log4j-api/2.3/log4j-api-2.3.jar:/home/monty/IdeaProjects/.m2/repository/org/apache/logging/log4j/log4j-core/2.3/log4j-core-2.3.jar:/home/monty/IdeaProjects/.m2/repository/org/slf4j/slf4j-api/1.7.25/slf4j-api-1.7.25.jar:/home/monty/IdeaProjects/.m2/repository/com/hazelcast/hazelcast-client-protocol/1.8.0-7/hazelcast-client-protocol-1.8.0-7.jar:/home/monty/IdeaProjects/.m2/repository/org/osgi/org.osgi.core/4.2.0/org.osgi.core-4.2.0.jar:/home/monty/IdeaProjects/.m2/repository/org/codehaus/groovy/groovy-all/2.1.8/groovy-all-2.1.8.jar:/home/monty/IdeaProjects/.m2/repository/org/jruby/jruby-complete/1.7.22/jruby-complete-1.7.22.jar:/home/monty/IdeaProjects/.m2/repository/javax/cache/cache-api/1.1.0/cache-api-1.1.0.jar:/home/monty/IdeaProjects/.m2/repository/com/google/code/findbugs/annotations/3.0.0/annotations-3.0.0.jar: -sourcepath /home/monty/IdeaProjects/java-corpus/hazelcast/hazelcast/src/main/java: -s /home/monty/IdeaProjects/hazelcast/hazelcast/target/generated-sources/annotations -g -target 1.8 -source 8 -encoding UTF-8 -parameters"));
    Collection<Diagnostic<? extends JavaFileObject>> diagnostics =
        DiagnosticsCollector.collectDiagnosticsFromScans(ImmutableList.of(scan), Verbosity.VERBOSE);

    diagnostics.forEach(System.out::println);
  }

  @Test
  public void parseIntoJDT() throws Exception {
    String code =
        new Formatter().formatSource("public class Foo {\n private List<Integer> i; \n }");

    List<String> lines = Splitter.on('\n').splitToList(code);
    SrcFile f = new SrcFile("foo.java", lines);

    AbstractJdtTreeGenerator treeGenerator =
        new AbstractJdtTreeGenerator() {
          @Override
          protected AbstractJdtVisitor createVisitor() {
            try {
              return new BetterJdtVisitor(new SrcFile("foo.java", lines));
            } catch (FormatterException e) {
              throw new RuntimeException(e);
            }
          }
        };

    TreeContext tree = treeGenerator.generateFromString(f.getSrc());

    System.out.println("done");
  }
}
