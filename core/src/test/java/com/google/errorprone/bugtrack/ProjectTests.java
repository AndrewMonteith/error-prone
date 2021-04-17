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

import static com.google.errorprone.bugtrack.BugComparers.trackIdentical;
import static com.google.errorprone.bugtrack.BugComparers.trackPosition;
import static com.google.errorprone.bugtrack.motion.trackers.DiagnosticPositionTrackers.newBetterIJMPosTracker;
import static com.google.errorprone.bugtrack.motion.trackers.DiagnosticPositionTrackers.newIJMStartAndEndTracker;

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
    CorpusProject project = new JSoupProject();

    DiagnosticsFile oldFile =
        DiagnosticsFile.load(
            project,
            "/home/monty/IdeaProjects/java-corpus/diagnostics/jsoup/19 fd46489cd718ddea7b28f89953265e7ce4ec8372");

    DiagnosticsFile newFile =
        DiagnosticsFile.load(
            project,
            "/home/monty/IdeaProjects/java-corpus/diagnostics/jsoup/81 21b01b258952fd6aeb55d061f98321e5b9fe93e0");

    BugComparerCtor justEndpoints =
        BugComparers.conditional(
            DiagnosticPredicates.canTrackIdentically(),
            trackIdentical(),
            trackPosition(newIJMStartAndEndTracker()));

    BugComparerCtor justSingle =
        BugComparers.conditional(
            DiagnosticPredicates.canTrackIdentically(),
            trackIdentical(),
            trackPosition(newBetterIJMPosTracker()));

    MatchResults results =
        DiagnosticsMatcher.fromFiles(project, oldFile, newFile, justEndpoints).match();

    System.out.println(results);
  }

  @Test
  public void trackProject() throws IOException {
    CorpusProject project = new GuiceProject();
    Path output = ProjectFiles.get("java-corpus/comparison_data/guice");
    List<GrainDiagFile> grainFiles =
        GrainDiagFile.loadSortedFiles(
            project, ProjectFiles.get("java-corpus/diagnostics/guice_full"));

    MultiGrainDiagFileComparer.compareFiles(project, output, grainFiles, true);
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
        new Formatter()
            .formatSource("public class Foo {\n void foo() {\n Class c = Foo.class; \n }\n }");

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
