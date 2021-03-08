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

import com.github.gumtreediff.gen.jdt.AbstractJdtVisitor;
import com.google.errorprone.bugtrack.DatasetDiagnostic;
import com.google.errorprone.bugtrack.motion.DiagPosEqualityOracle;
import com.google.errorprone.bugtrack.motion.DiagSrcPosEqualityOracle;
import com.google.errorprone.bugtrack.motion.SrcFilePair;
import com.google.errorprone.bugtrack.utils.IOThrowingSupplier;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public final class IJMStartAndEndPosTracker extends BaseIJMPosTracker implements DiagnosticPositionTracker {
    public IJMStartAndEndPosTracker(SrcFilePair srcFilePair,
                                    TrackersSharedState sharedState,
                                    IOThrowingSupplier<AbstractJdtVisitor> jdtVisitorSupplier) throws IOException {
        super(srcFilePair, sharedState, jdtVisitorSupplier);
    }

    public IJMStartAndEndPosTracker(SrcFilePair srcFilePair, TrackersSharedState sharedState) throws IOException {
        super(srcFilePair, sharedState);
    }

    @Override
    public Optional<DiagPosEqualityOracle> track(DatasetDiagnostic oldDiag) {
        Optional<NodeLocation> mappedStartPos = trackPosition(oldDiag.getStartPos());
        Optional<List<NodeLocation>> mappedEndPoses = trackEndPosition(oldDiag.getEndPos());

        if (!mappedStartPos.isPresent()) {
            return Optional.empty();
        }

        if (!mappedEndPoses.isPresent() || mappedEndPoses.get().isEmpty()) {
            return mappedStartPos.map(srcBufRange -> DiagSrcPosEqualityOracle.byStartAndEndPos(srcBufRange.start, srcBufRange.end));
        }

        List<DiagPosEqualityOracle> posOracles = mappedEndPoses.get().stream()
                .map(endPos -> DiagSrcPosEqualityOracle.byStartAndEndPos(mappedStartPos.get().start, endPos.end))
                .collect(Collectors.toList());

        return Optional.of(DiagPosEqualityOracle.any(posOracles));
    }
}
