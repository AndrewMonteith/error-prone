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

import java.util.Optional;

public class IJMPosTracker implements DiagnosticPositionTracker {
  private final SrcPairInfo srcPairInfo;

  public IJMPosTracker(SrcPairInfo srcPairInfo) {
    this.srcPairInfo = srcPairInfo;
  }

  @Override
  public boolean shouldAdjustPositions(DatasetDiagnostic oldDiagnostic) {
    return true;
  }

  @Override
  public Optional<DiagPosEqualityOracle> track(DatasetDiagnostic oldDiagnostic) {
    final long oldPos = oldDiagnostic.getPos();

    Optional<ITree> lowestNode =
        ITreeUtils.findLowestNodeEncompassing(srcPairInfo.getMatchedOldJdtTree(), (int) oldPos);
    if (!lowestNode.isPresent() || !lowestNode.get().isMatched()) {
      return Optional.empty();
    }

    return lowestNode.map(
        oldNode ->
            DiagSrcPosEqualityOracle.byPosition(
                getNewPosition(oldNode, oldPos, srcPairInfo.getMatchDst(oldNode))));
  }

  private long getNewPosition(ITree oldNode, final long oldPos, ITree newNode) {
    if (oldPos == oldNode.getPos()) {
      return newNode.getPos();
    } else if (oldPos == oldNode.getEndPos()) {
      return newNode.getEndPos();
    } else {
      final long newGuessedPos = newNode.getPos() + (oldPos - oldNode.getPos());
      return Math.max(newNode.getPos(), Math.min(newNode.getEndPos(), newGuessedPos));
    }
  }
}
