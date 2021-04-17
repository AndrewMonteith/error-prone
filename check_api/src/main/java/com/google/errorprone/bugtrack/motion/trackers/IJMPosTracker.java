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

import com.google.errorprone.bugtrack.DatasetDiagnostic;
import com.google.errorprone.bugtrack.SrcPairInfo;
import com.google.errorprone.bugtrack.motion.DiagPosEqualityOracle;
import com.google.errorprone.bugtrack.motion.DiagSrcPosEqualityOracle;

import java.io.IOException;
import java.util.Optional;

public final class IJMPosTracker extends BaseIJMPosTracker implements DiagnosticPositionTracker {
  public IJMPosTracker(SrcPairInfo srcPairInfo) throws IOException {
    super(srcPairInfo);
  }

  private long modifyAJLCOrJ7ApiDiagnostic(DatasetDiagnostic diagnostic) {
    // The AndroidJdkLibsChecker and Java7APIChecker position point to the '.' which is not included
    // in the JDT AST. Not including the '.''s in AST I imagine is to reduce the size of the tree so
    // we don't want to introduce them. These two diagnostics can point either at a symbol of a
    // method call. To discern between the two cases, if the diagnostic's position is the same as
    // it's start position then it points at a class symbol else it points at a '.'. So if it points
    // at a '.' then we knock it forward 1 so that hopefully it points at a node.
    String diagType = diagnostic.getType();
    final long diagPos = diagnostic.getPos();

    if (!(diagType.equals("AndroidJdkLibsChecker") || diagType.equals("Java7ApiChecker"))) {
      return diagPos;
    }

    if (diagnostic.getStartPos() == diagPos) {
      return diagPos;
    } else {
      return diagPos + 1;
    }
  }

  private long modifyPosition(DatasetDiagnostic diagnostic) {
    return modifyAJLCOrJ7ApiDiagnostic(diagnostic);
  }

  @Override
  public Optional<DiagPosEqualityOracle> track(DatasetDiagnostic oldDiag) {
    if (oldDiag.getPos() == -1) {
      return Optional.empty();
    }

    return trackStartPosition(modifyPosition(oldDiag))
        .map(srcBufferRange -> DiagSrcPosEqualityOracle.byPosition(srcBufferRange.pos));
  }
}
