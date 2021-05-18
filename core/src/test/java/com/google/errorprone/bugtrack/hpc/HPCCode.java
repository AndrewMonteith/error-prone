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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.errorprone.bugtrack.*;
import com.google.errorprone.bugtrack.harness.DiagnosticsFile;
import com.google.errorprone.bugtrack.harness.JavaLinesChangedFilter;
import com.google.errorprone.bugtrack.harness.ProjectHarness;
import com.google.errorprone.bugtrack.harness.Verbosity;
import com.google.errorprone.bugtrack.harness.evaluating.*;
import com.google.errorprone.bugtrack.harness.matching.DiagnosticsMatcher;
import com.google.errorprone.bugtrack.harness.matching.MatchResults;
import com.google.errorprone.bugtrack.TimingInformation;
import com.google.errorprone.bugtrack.harness.utils.CommitDAGPathFinders;
import com.google.errorprone.bugtrack.motion.trackers.DiagnosticPositionTrackerConstructor;
import com.google.errorprone.bugtrack.motion.trackers.DiagnosticPredicates;
import com.google.errorprone.bugtrack.projects.*;
import com.google.errorprone.bugtrack.utils.GitUtils;
import com.google.errorprone.bugtrack.utils.ProjectFiles;
import com.google.errorprone.bugtrack.utils.ThrowingBiConsumer;
import com.google.errorprone.bugtrack.utils.ThrowingConsumer;
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
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Future;
import java.util.stream.IntStream;

import static com.google.errorprone.bugtrack.BugComparers.conditional;
import static com.google.errorprone.bugtrack.BugComparers.*;
import static com.google.errorprone.bugtrack.harness.evaluating.BugComparerExperiment.withGit;
import static com.google.errorprone.bugtrack.motion.trackers.DiagnosticPositionTrackers.any;
import static com.google.errorprone.bugtrack.motion.trackers.DiagnosticPositionTrackers.*;

public final class HPCCode {
  private static final Map<String, CorpusProject> projects;

  static {
    Map<String, CorpusProject> projs = new HashMap<>();
    projs.put("guice", new GuiceProject());
    projs.put("jsoup", new JSoupProject());
    projs.put("metrics", new MetricsProject());
    projs.put("checkstyle", new CheckstyleProject());
    projs.put("junit4", new JUnitProject());
    projs.put("dubbo", new DubboProject());
    projs.put("hazelcast", new HazelcastProject());
    projs.put("mcMMO", new McMMOProject());
    projs.put("cobertura", new CoberturaProject());
    projs.put("jruby", new JRubyProject());

    projects = ImmutableMap.copyOf(projs);
  }

  public static void main(String[] args) throws GitAPIException, IOException, InterruptedException {
    Repository repo = new JRubyProject().loadRepo();
    CommitRange range =
        new CommitRange(
            "77d1af438a16fc8795446b63644cc63a25b32e06", "45a5f884a1a001493a67c240180182c646ff8a38");

    List<RevCommit> filteredCommits =
        new JavaLinesChangedFilter(new Git(repo), 200)
            .filter(CommitDAGPathFinders.in(repo, range).longest());

    System.out.println("Total commits  " + filteredCommits.size());
  }

  @Test
  public void runComparisons() throws Exception {
    String projectName = System.getProperty("project");

    CorpusProject project = loadProject();
    Path diagnostics = ProjectFiles.get("diagnostics/" + projectName);
    Path output = ProjectFiles.get("comparisons/" + projectName);

    BugComparerCtor comparer1 =
        and(
            matchProblem(),
            BugComparers.conditional(
                DiagnosticPredicates.canTrackIdenticalLocation(),
                matchIdenticalLocation(),
                trackPosition(newIJMPosTracker())));

    BugComparerCtor comparer2 =
        and(
            matchProblem(),
            BugComparers.conditional(
                DiagnosticPredicates.canTrackIdenticalLocation(),
                matchIdenticalLocation(),
                trackPosition(newIJMStartAndEndTracker())));

    BugComparerExperiment.forProject(project)
        .withData(RandomDiagFilePairLoader.allFiles(project, diagnostics))
        .comparePaths(withGit(project, GitPathComparer::new))
        .loadDiags(withGit(project, GitSrcFilePairLoader::new))
        .setBugComparer1(comparer1)
        .setBugComparer2(comparer2)
        .findMissedTrackings(MissedLikelihoodCalculators.diagLineSrcOverlap())
        .trials(20)
        .run(output);
  }

  @Before
  public void initJGit() {
    FS.DETECTED.setGitSystemConfig(
        Paths.get("/home/monty/IdeaProjects/java-corpus/git/etc/gitconfig").toFile());
  }

  private Path getPath(String path1, String... paths) {
    Path p = Paths.get(path1, paths);
    if (!p.toFile().exists()) {
      throw new RuntimeException(p + " does not exist");
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

    System.out.println(
        "Parsing commit " + commit.getName() + " for project " + System.getProperty("project"));
    System.out.println("Saving response into " + outputFile);

    new ProjectHarness(project, Verbosity.VERBOSE).serialiseCommit(commit, outputFile);
  }

  @Test
  public void genCommits() throws GitAPIException, IOException {
    Repository repo = projects.get(System.getProperty("project")).loadRepo();
    CommitRange range =
        new CommitRange(System.getProperty("oldCommit"), System.getProperty("newCommit"));

    List<RevCommit> filteredCommits =
        new JavaLinesChangedFilter(
                new Git(repo), Integer.parseInt(System.getProperty("linesChanged")))
            .filter(CommitDAGPathFinders.in(repo, range).dfs());

    System.out.println("Total commits  " + filteredCommits.size());

    for (int i = 0; i < filteredCommits.size(); ++i) {
      System.out.println(i + " " + filteredCommits.get(i).getName());
    }
  }

  @Test
  public void betweenCommits() throws GitAPIException, IOException, InterruptedException {
    CorpusProject project = loadProject();
    CommitRange range =
        new CommitRange(System.getProperty("oldCommit"), System.getProperty("newCommit"));

    int linesChangedThreshold =
        System.getProperty("linesChanged") == null
            ? 50
            : Integer.parseInt(System.getProperty("linesChanged"));

    System.out.println("Lines changed threshold " + linesChangedThreshold);
    System.out.println("Number of cores " + Runtime.getRuntime().availableProcessors());

    new ProjectHarness(project)
        .serialiseCommits(
            range,
            new JavaLinesChangedFilter(new Git(project.loadRepo()), linesChangedThreshold),
            getPath(System.getProperty("outputFolder")),
            Integer.parseInt(System.getProperty("offset")));
  }

  private static ImmutableList<DiagnosticsFile> loadInterleavedDiagnosticsFiles(
      String projectName, CorpusProject project) throws IOException {
    return GrainDiagFile.loadSortedFiles(
            project, ProjectFiles.get("diagnostics/").resolve(projectName))
        .stream()
        .map(GrainDiagFile::getDiagFile)
        .collect(ImmutableList.toImmutableList());
  }

  @Test
  public void performSequentialComparisons() throws Exception {
    String projectName = System.getProperty("project");
    CorpusProject project = loadProject();

    List<GrainDiagFile> grainFiles =
        GrainDiagFile.loadSortedFiles(
            project, ProjectFiles.get("diagnostics/").resolve(projectName + "_full"));

    Path output = ProjectFiles.get("comparison_data/").resolve(projectName);
    boolean inParallel = System.getProperty("inParallel").equals("true");

    MultiGrainDiagFileComparer.compareFiles(project, output, grainFiles, inParallel);
  }

  private static <T> void runTasksInParallel(Collection<Callable<T>> tasks) {
    new ForkJoinPool().invokeAll(tasks).forEach((ThrowingConsumer<Future<T>>) Future::get);
  }

  private static Collection<Callable<Void>> createScanTasks(
      CorpusProject project,
      List<DiagnosticsFile> diagFiles,
      BugComparerCtor comparer,
      Path output) {

    return IntStream.range(0, diagFiles.size() - 1)
        .mapToObj(
            i ->
                (Callable<Void>)
                    () -> {
                      DiagnosticsFile before = diagFiles.get(i);
                      DiagnosticsFile after = diagFiles.get(i + 1);

                      String comparison = before.name + " -> " + after.name;
                      System.out.println(comparison);

                      DiagnosticsMatcher.fromFiles(project, before, after, comparer)
                          .match()
                          .save(output.resolve(comparison));

                      return null;
                    })
        .collect(ImmutableList.toImmutableList());
  }

  @Test
  public void scanInterleavedCommits() throws Exception {
    String projectName = System.getProperty("project");
    CorpusProject project = loadProject();

    Path output = ProjectFiles.get("comparison_data/").resolve(projectName);

    ImmutableList<DiagnosticsFile> diagFiles =
        loadInterleavedDiagnosticsFiles(projectName, project);

    runTasksInParallel(createScanTasks(project, diagFiles, DEFAULT_COMPARER, output));
  }

  @Test
  public void tryManyPositionTrackers_Lowres() {
    ImmutableMap<String, DiagnosticPositionTrackerConstructor> positionTrackers =
        ImmutableMap.of(
            "character_line_tracker",
            newCharacterLineTracker(),
            "token_line_tracker",
            newTokenizedLineTracker(),
            "ijm_pos_tracker",
            newIJMPosTracker(),
            "ijm_start_and_end",
            newIJMStartAndEndTracker(),
            "ijm_joint",
            any(newIJMPosTracker(), newIJMStartAndEndTracker()));

    Iterator<Integer> grainSizes =
        ImmutableList.of(50, 50, 25, 50, 50, 500, 50, 200, 50, 100).iterator();

    List<Callable<Void>> allTasks = new ArrayList<>();

    projects.forEach(
        (ThrowingBiConsumer<String, CorpusProject>)
            (projectName, project) -> {
              final int maxGrain = grainSizes.next();

              ImmutableList<DiagnosticsFile> diagFiles =
                  GrainDiagFile.loadSortedFiles(
                          project,
                          ProjectFiles.get("java-corpus/diagnostics/").resolve(projectName))
                      .stream()
                      .filter(grainFile -> grainFile.hasGrain(maxGrain))
                      .map(GrainDiagFile::getDiagFile)
                      .collect(ImmutableList.toImmutableList());

              positionTrackers.forEach(
                  (trackerName, tracker) -> {
                    BugComparerCtor comparer =
                        and(
                            matchProblem(),
                            conditional(
                                DiagnosticPredicates.canTrackIdenticalLocation(),
                                matchIdenticalLocation(),
                                trackPosition(tracker)));

                    IntStream.range(0, diagFiles.size() - 1)
                        .forEach(
                            i ->
                                allTasks.add(
                                    () -> {
                                      DiagnosticsFile before = diagFiles.get(i);
                                      DiagnosticsFile after = diagFiles.get(i + 1);

                                      StringBuilder timerOutput = new StringBuilder();
                                      timerOutput
                                          .append("Benchmark ")
                                          .append(projectName)
                                          .append(" ")
                                          .append(trackerName)
                                          .append(" ")
                                          .append(before.name)
                                          .append(" ")
                                          .append(after.name)
                                          .append("\n");

                                      for (int run = 0; run < 7; ++run) {
                                        try {
                                          TimingInformation timeInformation =
                                              new TimingInformation();

                                          DiagnosticsMatcher matcher =
                                              DiagnosticsMatcher.fromFiles(
                                                  project,
                                                  before,
                                                  after,
                                                  comparer,
                                                  timeInformation);

                                          matcher.match();

                                          if (run == 0) {
                                            continue; // warmup
                                          }

                                          timerOutput
                                              .append(
                                                  timeInformation.timeSpentComparingChangedFiles)
                                              .append(" ")
                                              .append(
                                                  timeInformation.timeSpentComparingUnchangedFiles)
                                              .append("\n");

                                          if (run == 6) {
                                            timerOutput
                                                .append(timeInformation.diagnosticsInChangedFiles)
                                                .append(" ")
                                                .append(timeInformation.diagnosticsInUnchangedFiles)
                                                .append(" ")
                                                .append(timeInformation.totalChangedLinesProcessed)
                                                .append(" ")
                                                .append(
                                                    timeInformation.totalUnchangedLinesProcessed)
                                                .append("\n");
                                          }
                                        } catch (IOException | GitAPIException e) {
                                          e.printStackTrace();
                                          timerOutput.append("FAILED\n");
                                        }
                                      }

                                      System.out.println(timerOutput);
                                      return null;
                                    }));
                  });
            });

    System.out.println("Tasks " + allTasks.size());
    //    tasks.forEach((ThrowingConsumer<Callable<Void>>) Callable::call);
    runTasksInParallel(allTasks);
  }

  @Test
  public void findChangingMessages() throws Exception {
    Path allDiagnostics = Paths.get("/home/monty/IdeaProjects/diagnostics");

    BugComparerCtor specialMatcher =
        srcPairInfo ->
            (oldDiag, newDiag) ->
                (!srcPairInfo.files.srcChanged)
                    && oldDiag.getLineNumber() == newDiag.getLineNumber()
                    && oldDiag.getColumnNumber() == newDiag.getColumnNumber()
                    && oldDiag.getStartPos() == newDiag.getStartPos()
                    && oldDiag.getPos() == newDiag.getPos()
                    && oldDiag.getEndPos() == newDiag.getEndPos()
                    && oldDiag.getType().equals(newDiag.getType())
                    && !oldDiag.getMessage().equals(newDiag.getMessage());

    projects.forEach(
        (name, project) -> {
          System.out.println("Scanning " + name);

          List<DiagnosticsFile> files =
              ProjectDiagnosticsFolder.load(project, allDiagnostics.resolve(name));

          for (int i = 0; i < files.size() - 1; ++i) {
            DiagnosticsFile before = files.get(i);
            DiagnosticsFile after = files.get(i + 1);

            System.out.println(before.commitId + " -> " + after.commitId);

            try {
              MatchResults results =
                  DiagnosticsMatcher.fromFiles(project, before, after, specialMatcher).match();

              results
                  .getMatchedDiagnostics()
                  .forEach(
                      (beforeDiag, afterDiag) -> {
                        System.out.println("Weird match");
                        System.out.println(beforeDiag);
                        System.out.println(afterDiag);
                      });
            } catch (IOException | GitAPIException e) {
              e.printStackTrace();
            }
          }
        });
  }
}
