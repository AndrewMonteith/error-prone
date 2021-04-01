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

import com.google.errorprone.bugtrack.BugComparer;
import com.google.errorprone.bugtrack.PathsComparer;
import com.google.errorprone.bugtrack.SrcFilePairLoader;

public final class BugComparerEvaluationConfig {
  private final DiagnosticsFilePairMapper<BugComparer> bugComparer1Ctor;
  private final DiagnosticsFilePairMapper<BugComparer> bugComparer2Ctor;
  private final DiagnosticsFilePairMapper<SrcFilePairLoader> srcFilePairLoader;
  private final DiagnosticsFilePairMapper<PathsComparer> pathsComparer;

  public BugComparerEvaluationConfig(
      DiagnosticsFilePairMapper<BugComparer> bugComparer1Ctor,
      DiagnosticsFilePairMapper<BugComparer> bugComparer2Ctor,
      DiagnosticsFilePairMapper<SrcFilePairLoader> srcFilePairLoader,
      DiagnosticsFilePairMapper<PathsComparer> pathsComparer) {
    this.bugComparer1Ctor = bugComparer1Ctor;
    this.bugComparer2Ctor = bugComparer2Ctor;
    this.srcFilePairLoader = srcFilePairLoader;
    this.pathsComparer = pathsComparer;
  }

  public BugComparer createBugComparer1(DiagnosticsFilePairLoader.Pair oldAndNewFile)
      throws Exception {
    return bugComparer1Ctor.apply(oldAndNewFile);
  }

  public BugComparer createBugComparer2(DiagnosticsFilePairLoader.Pair oldAndNewFile)
      throws Exception {
    return bugComparer2Ctor.apply(oldAndNewFile);
  }

  public SrcFilePairLoader createSrcFilePairLoader(DiagnosticsFilePairLoader.Pair oldAndNewFile)
      throws Exception {
    return srcFilePairLoader.apply(oldAndNewFile);
  }

  public PathsComparer createPathComparer(DiagnosticsFilePairLoader.Pair oldAndNewFile)
      throws Exception {
    return pathsComparer.apply(oldAndNewFile);
  }
}
