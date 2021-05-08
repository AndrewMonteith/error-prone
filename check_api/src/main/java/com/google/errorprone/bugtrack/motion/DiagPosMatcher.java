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

package com.google.errorprone.bugtrack.motion;

import com.github.gumtreediff.tree.ITree;
import com.google.errorprone.bugtrack.BugComparer;
import com.google.errorprone.bugtrack.DatasetDiagnostic;
import com.google.errorprone.bugtrack.SrcPairInfo;
import com.google.errorprone.bugtrack.motion.trackers.DiagnosticPositionTracker;
import com.google.errorprone.bugtrack.motion.trackers.ITreeUtils;

import java.util.Optional;

public class DiagPosMatcher implements BugComparer {
  private final SrcPairInfo srcPairInfo;
  private final DiagnosticPositionTracker posTracker;

  public DiagPosMatcher(SrcPairInfo srcPairInfo, DiagnosticPositionTracker posTracker) {
    this.srcPairInfo = srcPairInfo;
    this.posTracker = posTracker;
  }

  @Override
  public boolean areSame(DatasetDiagnostic oldDiagnostic, DatasetDiagnostic newDiagnostic) {
    if (!oldDiagnostic.isSameType(newDiagnostic)) {
      return false;
    }

    if (oldDiagnostic.getLineNumber() == -1 || newDiagnostic.getLineNumber() == -1) {
      return false;
    }

    // Some commits we failed to get the end position? Weird effect outside of our control
    if (oldDiagnostic.getEndPos() == -1) {
      oldDiagnostic =
          DiagnosticPositionModifiers.recoverEndPosition(
              oldDiagnostic, srcPairInfo.getOldJdtTree());
    }

    if (newDiagnostic.getEndPos() == -1) {
      newDiagnostic =
          DiagnosticPositionModifiers.recoverEndPosition(
              newDiagnostic, srcPairInfo.getNewJdtTree());
    }

    if (posTracker.shouldAdjustPositions(oldDiagnostic)) {
      oldDiagnostic =
          DiagnosticPositionModifiers.modify(srcPairInfo.getOldJdtTree(), oldDiagnostic);
      newDiagnostic =
          DiagnosticPositionModifiers.modify(srcPairInfo.getNewJdtTree(), newDiagnostic);
    }

    try {
      Optional<DiagPosEqualityOracle> posEqOracle = posTracker.track(oldDiagnostic);

      return posEqOracle.isPresent() && posEqOracle.get().hasSamePosition(newDiagnostic);
    } catch (Exception e) {
      e.printStackTrace();
      return false;
    }
  }
}
