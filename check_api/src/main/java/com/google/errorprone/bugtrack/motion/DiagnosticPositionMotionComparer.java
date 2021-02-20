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

import com.github.difflib.algorithm.DiffException;
import com.google.errorprone.bugtrack.BugComparer;
import com.google.errorprone.bugtrack.DatasetDiagnostic;
import com.google.errorprone.bugtrack.motion.trackers.DiagnosticPositionTracker;
import com.google.errorprone.bugtrack.motion.trackers.DiagnosticPositionTrackerConstructor;
import com.google.errorprone.bugtrack.motion.trackers.TrackersSharedState;
import com.google.errorprone.bugtrack.utils.MemoMap;

import java.io.IOException;
import java.util.Optional;

public class DiagnosticPositionMotionComparer implements BugComparer {
    private final DiagnosticsDeltaManager diagnosticsDeltaManager;
    private final DiagnosticPositionTrackerConstructor trackerConstructor;

    private final MemoMap<String, DiagnosticPositionTracker> positionTrackers;
    private final TrackersSharedState sharedState;

    public DiagnosticPositionMotionComparer(DiagnosticsDeltaManager diagnosticsDeltaManager,
                                            DiagnosticPositionTrackerConstructor trackerConstructor) {
        this.diagnosticsDeltaManager = diagnosticsDeltaManager;
        this.trackerConstructor = trackerConstructor;
        this.positionTrackers = new MemoMap<>();
        this.sharedState = new TrackersSharedState();
    }

    private DiagnosticPositionTracker createDiagnosticPositionTracker(DatasetDiagnostic oldDiagnostic,
                                                                      DatasetDiagnostic newDiagnostic) throws DiffException, IOException {
        return trackerConstructor.create(
                diagnosticsDeltaManager.loadFilesBetweenDiagnostics(oldDiagnostic, newDiagnostic), sharedState);
    }

    private DiagnosticPositionTracker getDiagnosticPositionTracker(DatasetDiagnostic oldDiagnostic,
                                                                   DatasetDiagnostic newDiagnostic) throws Exception {
        return positionTrackers.getOrInsertThatCouldThrow(oldDiagnostic.getFileName(),
                () -> createDiagnosticPositionTracker(oldDiagnostic, newDiagnostic));
    }

    @Override
    public boolean areSame(DatasetDiagnostic oldDiagnostic,
                           DatasetDiagnostic newDiagnostic) {
        if (!(diagnosticsDeltaManager.inSameFile(oldDiagnostic, newDiagnostic) && oldDiagnostic.isSameType(newDiagnostic))) {
            return false;
        }

        try {
            DiagnosticPositionTracker posTracker = getDiagnosticPositionTracker(oldDiagnostic, newDiagnostic);

            Optional<DiagPosEqualityOracle> posEqOracle = posTracker.track(oldDiagnostic);

            return posEqOracle.isPresent() && posEqOracle.get().hasSamePosition(newDiagnostic);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
