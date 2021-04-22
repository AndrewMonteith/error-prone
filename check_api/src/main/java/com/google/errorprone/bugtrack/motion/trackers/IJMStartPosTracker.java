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

public final class IJMStartPosTracker extends BaseIJMPosTracker
    implements DiagnosticPositionTracker {
  public IJMStartPosTracker(SrcPairInfo srcPairInfo) throws IOException {
    super(srcPairInfo);
  }

  @Override
  public Optional<DiagPosEqualityOracle> track(DatasetDiagnostic oldDiag) {
    return trackStartNodePosition(oldDiag.getStartPos())
        .map(srcBufferRange -> DiagSrcPosEqualityOracle.byStartPos(srcBufferRange.startPos));
  }
}
