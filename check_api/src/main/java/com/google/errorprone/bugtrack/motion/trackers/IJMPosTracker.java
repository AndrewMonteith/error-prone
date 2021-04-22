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
import java.util.function.Predicate;

import static com.google.errorprone.bugtrack.motion.trackers.ITreeUtils.findClosestMatchedNodeThat;

public class IJMPosTracker extends BaseIJMPosTracker implements DiagnosticPositionTracker {
  public IJMPosTracker(SrcPairInfo srcPairInfo) throws IOException {
    super(srcPairInfo);
  }

  private Optional<ITree> getMatching(Predicate<ITree> nodeTest) {
    return findClosestMatchedNodeThat(oldSrcTree.getRoot(), nodeTest).map(mappings::getDst);
  }

  private Optional<DiagPosEqualityOracle> trackPointDiagnostic(final long pos) {
    return getMatching(node -> node.getPos() == pos)
        .map(node -> DiagSrcPosEqualityOracle.byPosition(node.getPos()));
  }

  private long modifySpecificPosition(DatasetDiagnostic diagnostic) {
    switch (diagnostic.getType()) {
      case "AndroidJdkLibsChecker":
      case "Java7ApiChecker:":
        return diagnostic.getPos() + 1;
      default:
        return diagnostic.getPos();
    }
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

  @Override
  public Optional<DiagPosEqualityOracle> track(DatasetDiagnostic oldDiagnostic) {
    if (oldDiagnostic.getStartPos() == oldDiagnostic.getEndPos()) {
      return trackPointDiagnostic(oldDiagnostic.getStartPos());
    }

    final long oldPos = modifySpecificPosition(oldDiagnostic);

    return findClosestMatchedNodeThat(
            oldSrcTree.getRoot(), node -> ITreeUtils.encompasses(node, oldPos))
        .map(
            oldNode ->
                DiagSrcPosEqualityOracle.byPosition(
                    getNewPosition(oldNode, oldPos, mappings.getDst(oldNode))));
  }
}
