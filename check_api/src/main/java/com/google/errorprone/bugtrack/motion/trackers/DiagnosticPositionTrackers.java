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

import com.google.errorprone.bugtrack.Lazy;
import com.google.errorprone.bugtrack.motion.DiagPosEqualityOracle;
import com.google.errorprone.bugtrack.utils.ThrowingSupplier;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class DiagnosticPositionTrackers {
  private DiagnosticPositionTrackers() {}

  public static DiagnosticPositionTrackerConstructor newCharacterLineTracker() {
    return srcPairInfo ->
        new CharacterLineTracker(
            srcPairInfo.files.oldFile.getLines(), srcPairInfo.files.newFile.getLines());
  }

  public static DiagnosticPositionTrackerConstructor newTokenizedLineTracker() {
    return srcPairInfo ->
        new TokenizedLineTracker(
            TokenizedLine.tokenizeSrc(
                srcPairInfo.files.oldFile.getLines(), srcPairInfo.loadOldJavacContext()),
            TokenizedLine.tokenizeSrc(
                srcPairInfo.files.newFile.getLines(), srcPairInfo.loadNewJavacContext()));
  }

  public static DiagnosticPositionTrackerConstructor newIJMStartPosTracker() {
    return IJMStartPosTracker::new;
  }

  public static DiagnosticPositionTrackerConstructor newIJMStartAndEndTracker() {
    return IJMStartAndEndPosTracker::new;
  }

  public static DiagnosticPositionTrackerConstructor conditional(
      DiagnosticPredicates.Predicate predicate,
      DiagnosticPositionTrackerConstructor comparerIfTrue,
      DiagnosticPositionTrackerConstructor comparerIfFalse) {
    return srcPairInfo -> {
      LazyTracker lazyTrueComparer = new LazyTracker(() -> comparerIfTrue.create(srcPairInfo));
      LazyTracker lazyFalseComparer = new LazyTracker(() -> comparerIfFalse.create(srcPairInfo));

      return oldDiag -> {
        DiagnosticPositionTracker tracker =
            predicate.test(srcPairInfo.files.oldFile, oldDiag)
                ? lazyTrueComparer.get()
                : lazyFalseComparer.get();

        return tracker.track(oldDiag);
      };
    };
  }

  public static DiagnosticPositionTrackerConstructor newIJMPosTracker() {
    return IJMPosTracker::new;
  }

  public static DiagnosticPositionTrackerConstructor first(
      DiagnosticPositionTrackerConstructor... trackerCtors) {
    return srcPairInfo -> {
      List<DiagnosticPositionTracker> trackers = new ArrayList<>();
      for (DiagnosticPositionTrackerConstructor trackerCtor : trackerCtors) {
        trackers.add(trackerCtor.create(srcPairInfo));
      }

      return diagnostic -> {
        for (DiagnosticPositionTracker tracker : trackers) {
          Optional<DiagPosEqualityOracle> posEqOracle = tracker.track(diagnostic);
          if (posEqOracle.isPresent()) {
            return posEqOracle;
          }
        }

        return Optional.empty();
      };
    };
  }

  public static DiagnosticPositionTrackerConstructor any(
      DiagnosticPositionTrackerConstructor... trackerCtors) {
    return srcPairInfo -> {
      final List<DiagnosticPositionTracker> trackers = new ArrayList<>();
      for (DiagnosticPositionTrackerConstructor trackerCtor : trackerCtors) {
        trackers.add(trackerCtor.create(srcPairInfo));
      }

      return oldDiag -> {
        List<Optional<DiagPosEqualityOracle>> equalityOracles = new ArrayList<>();

        return Optional.of(
            newDiag -> {
              for (int i = 0; i < trackers.size(); ++i) {
                if (equalityOracles.size() <= i) {
                  equalityOracles.add(i, trackers.get(i).track(oldDiag));
                }

                Optional<DiagPosEqualityOracle> eqOracle = equalityOracles.get(i);
                if (eqOracle.isPresent() && eqOracle.get().hasSamePosition(newDiag)) {
                  return true;
                }
              }

              return false;
            });
      };
    };
  }

  private static class LazyTracker extends Lazy<DiagnosticPositionTracker> {
    public LazyTracker(ThrowingSupplier<DiagnosticPositionTracker> supplier) {
      super(supplier);
    }
  }
  //
  //  private static class LazyConstructor extends Lazy<DiagnosticPositionTracker> {
  //    public LazyConstructor(DiagnosticPositionTrackerConstructor ctor) {
  //
  //    }
  //
  //    private final DiagnosticPositionTrackerConstructor ctor;
  //    private DiagnosticPositionTracker tracker;
  //
  //    public LazyConstructor(DiagnosticPositionTrackerConstructor ctor) {
  //      this.ctor = ctor;
  //    }
  //
  //    public DiagnosticPositionTracker get(SrcFilePair srcFilePair, TrackersSharedState
  // sharedState) {
  //      if (tracker == null) {
  //        try {
  //          tracker = ctor.create(srcFilePair, sharedState);
  //        } catch (DiffException | IOException e) {
  //          throw new RuntimeException(e);
  //        }
  //      }
  //
  //      return tracker;
  //    }
  //  }
}
