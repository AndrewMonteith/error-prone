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

import com.github.difflib.algorithm.DiffException;
import com.google.errorprone.bugtrack.DatasetDiagnostic;
import com.google.errorprone.bugtrack.motion.DiagPosEqualityOracle;
import com.google.errorprone.bugtrack.motion.DiagSrcPosEqualityOracle;

import java.util.List;
import java.util.Optional;

/*

*/
public class CharacterLineTracker implements DiagnosticPositionTracker {
  private final SrcLineTracker<String> srcTracker;

  public CharacterLineTracker(List<String> oldSrc, List<String> newSrc) throws DiffException {
    this.srcTracker = new SrcLineTracker<>(oldSrc, newSrc);
  }

  @Override
  public Optional<DiagPosEqualityOracle> track(DatasetDiagnostic diag) {
    return srcTracker
        .getNewLineNumber(diag.getLineNumber())
        .map(newLineNum -> DiagSrcPosEqualityOracle.byLineCol(newLineNum, diag.getColumnNumber()));
  }
}
