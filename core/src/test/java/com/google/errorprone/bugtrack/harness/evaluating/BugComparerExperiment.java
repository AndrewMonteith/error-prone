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

package com.google.errorprone.bugtrack.harness.evaluating;

import com.google.errorprone.bugtrack.BugComparerCtor;
import com.google.errorprone.bugtrack.PathsComparer;
import com.google.errorprone.bugtrack.SrcFilePairLoader;
import com.google.errorprone.bugtrack.projects.CorpusProject;
import com.google.errorprone.bugtrack.utils.GitUtils;
import com.google.errorprone.bugtrack.utils.ThrowingTriFunction;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;

import java.nio.file.Path;
import java.nio.file.Paths;

/*
   This file's (well all my files) code quality is pants. You have been warned.
   It is purely helper functions for testing, ie a type-safe shoot yourself in the foot DSL
*/
public final class BugComparerExperiment {
  private final CorpusProject project;
  private DiagnosticsFilePairLoader dataLoader;
  private BugComparerCtor bugComparer1Ctor;
  private BugComparerCtor bugComparer2Ctor;
  private DiagnosticsFilePairMapper<SrcFilePairLoader> srcDiagLoader;
  private DiagnosticsFilePairMapper<PathsComparer> pathsComparerCtor;
  private int numOfTrials = 100;
  private MissedLikelihoodCalculator likelihoodCalc = MissedLikelihoodCalculators.zero();

  private BugComparerExperiment(CorpusProject project) {
    this.project = project;
  }

  public static <V> DiagnosticsFilePairMapper<V> withGit(
      CorpusProject project, ThrowingTriFunction<Repository, RevCommit, RevCommit, V> func) {
    return oldAndNewDiagFiles -> {
      Repository repo = project.loadRepo();

      return func.apply(
          repo,
          GitUtils.parseCommit(repo, oldAndNewDiagFiles.oldFile.commitId),
          GitUtils.parseCommit(repo, oldAndNewDiagFiles.newFile.commitId));
    };
  }

  public static BugComparerExperiment forProject(CorpusProject project) {
    return new BugComparerExperiment(project);
  }

  public BugComparerExperiment withData(DiagnosticsFilePairLoader dataLoader) {
    this.dataLoader = dataLoader;

    return this;
  }

  public BugComparerExperiment setBugComparer1(BugComparerCtor bugComparer1Ctor) {
    this.bugComparer1Ctor = bugComparer1Ctor;
    return this;
  }

  public BugComparerExperiment setBugComparer2(BugComparerCtor bugComparer2Ctor) {
    this.bugComparer2Ctor = bugComparer2Ctor;
    return this;
  }

  public BugComparerExperiment loadDiags(
      DiagnosticsFilePairMapper<SrcFilePairLoader> srcDiagLoader) {
    this.srcDiagLoader = srcDiagLoader;
    return this;
  }

  public BugComparerExperiment comparePaths(
      DiagnosticsFilePairMapper<PathsComparer> pathsComparerCtor) {
    this.pathsComparerCtor = pathsComparerCtor;
    return this;
  }

  public BugComparerExperiment findMissedTrackings(MissedLikelihoodCalculator likelihoodCalc) {
    this.likelihoodCalc = likelihoodCalc;
    return this;
  }

  public void run(Path output) throws Exception {
    if (!output.toFile().isDirectory()) {
      throw new RuntimeException(output + " does not exist");
    }

    BugComparerEvaluator eval =
        new BugComparerEvaluator(project, dataLoader, likelihoodCalc, numOfTrials);
    BugComparerEvaluationConfig evaluationConfig =
        new BugComparerEvaluationConfig(
            bugComparer1Ctor, bugComparer2Ctor, srcDiagLoader, pathsComparerCtor);

    eval.compareBugComparers(evaluationConfig, output);
  }

  public void run(String output) throws Exception {
    run(Paths.get(output));
  }

  public BugComparerExperiment trials(int trials) {
    this.numOfTrials = trials;
    return this;
  }
}
