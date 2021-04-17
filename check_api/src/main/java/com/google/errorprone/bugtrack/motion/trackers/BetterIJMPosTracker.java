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

package com.google.errorprone.bugtrack.motion.trackers;

import com.github.gumtreediff.tree.ITree;
import com.google.errorprone.bugtrack.DatasetDiagnostic;
import com.google.errorprone.bugtrack.SrcPairInfo;
import com.google.errorprone.bugtrack.motion.DiagPosEqualityOracle;
import com.google.errorprone.bugtrack.motion.DiagSrcPosEqualityOracle;

import java.io.IOException;
import java.util.Optional;

public class BetterIJMPosTracker extends BaseIJMPosTracker implements DiagnosticPositionTracker {
  private final PreferredPositionMap oldPrefMap;
  private final PreferredPositionMap newPrefMap;

  public BetterIJMPosTracker(SrcPairInfo srcPairInfo) throws IOException {
    super(srcPairInfo);

    this.oldPrefMap = new PreferredPositionMap(srcPairInfo.loadOldJavacAST());
    this.newPrefMap = new PreferredPositionMap(srcPairInfo.loadNewJavacAST());
  }

  @Override
  public Optional<DiagPosEqualityOracle> track(DatasetDiagnostic oldDiagnostic) {
    Optional<ITree> oldMatching =
        ITreeUtils.findClosestNodeThat(
            oldSrcTree.getRoot(),
            oldNode -> oldPrefMap.getPreferredPosition(oldNode) == oldDiagnostic.getPos());

    return oldMatching.map(
        oldNode ->
            DiagSrcPosEqualityOracle.byPosition(
                newPrefMap.getPreferredPosition(mappings.getDst(oldNode))));
  }
}
