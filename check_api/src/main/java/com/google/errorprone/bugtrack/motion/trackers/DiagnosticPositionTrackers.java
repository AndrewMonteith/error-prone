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
import com.github.gumtreediff.gen.jdt.AbstractJdtVisitor;
import com.google.errorprone.bugtrack.motion.DiagPosEqualityOracle;
import com.google.errorprone.bugtrack.motion.SrcFile;
import com.google.errorprone.bugtrack.motion.SrcFilePair;
import com.google.errorprone.bugtrack.utils.IOThrowingFunction;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public final class DiagnosticPositionTrackers {
  private DiagnosticPositionTrackers() {}

  public static DiagnosticPositionTrackerConstructor newCharacterLineTracker() {
    return (srcFilePair, sharedState) ->
        new CharacterLineTracker(srcFilePair.oldFile.getLines(), srcFilePair.newFile.getLines());
  }

  public static DiagnosticPositionTrackerConstructor newTokenizedLineTracker() {
    return (srcFilePair, sharedState) ->
        new TokenizedLineTracker(
            TokenizedLine.tokenizeSrc(
                srcFilePair.oldFile.getLines(), sharedState.loadOldJavacContext(srcFilePair)),
            TokenizedLine.tokenizeSrc(
                srcFilePair.newFile.getLines(), sharedState.loadNewJavacContext(srcFilePair)));
  }

  public static DiagnosticPositionTrackerConstructor newIJMStartPosTracker() {
    return IJMStartPosTracker::new;
  }

  public static DiagnosticPositionTrackerConstructor newIJMStartPosTracker(
      IOThrowingFunction<SrcFile, AbstractJdtVisitor> jdtVisitorSupplier) {
    return (srcFilePair, sharedState) ->
        new IJMStartPosTracker(srcFilePair, sharedState, jdtVisitorSupplier);
  }

  public static DiagnosticPositionTrackerConstructor newIJMStartAndEndTracker() {
    return IJMStartAndEndPosTracker::new;
  }

  public static DiagnosticPositionTrackerConstructor specific(
      Collection<String> diagTypes1,
      DiagnosticPositionTrackerConstructor comparerForType1s,
      DiagnosticPositionTrackerConstructor comparerForOthers) {
    return (srcFilePair, sharedState) -> {
      LazyConstructor lazyTracker1 = new LazyConstructor(comparerForType1s);
      LazyConstructor lazyTrackerOthers = new LazyConstructor(comparerForOthers);

      return (diag) -> {
        if (diagTypes1.contains(diag.getType())) {
          return lazyTracker1.get(srcFilePair, sharedState).track(diag);
        } else {
          return lazyTrackerOthers.get(srcFilePair, sharedState).track(diag);
        }
      };
    };
  }

  public static DiagnosticPositionTrackerConstructor newIJMPosTracker() {
    return IJMPosTracker::new;
  }

  public static DiagnosticPositionTrackerConstructor newIJMStartAndEndTracker(
      IOThrowingFunction<SrcFile, AbstractJdtVisitor> jdtVisitorSupplier) {
    return (srcFilePair, sharedState) ->
        new IJMStartAndEndPosTracker(srcFilePair, sharedState, jdtVisitorSupplier);
  }

  public static DiagnosticPositionTrackerConstructor first(
      DiagnosticPositionTrackerConstructor... trackerCtors) {
    return (srcFilePair, sharedState) -> {
      List<DiagnosticPositionTracker> trackers = new ArrayList<>();
      for (DiagnosticPositionTrackerConstructor trackerCtor : trackerCtors) {
        trackers.add(trackerCtor.create(srcFilePair, sharedState));
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
    return (srcFilePair, sharedState) -> {
      final List<DiagnosticPositionTracker> trackers = new ArrayList<>();
      for (DiagnosticPositionTrackerConstructor trackerCtor : trackerCtors) {
        trackers.add(trackerCtor.create(srcFilePair, sharedState));
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

  private static class LazyConstructor {
    private final DiagnosticPositionTrackerConstructor ctor;
    private DiagnosticPositionTracker tracker;

    public LazyConstructor(DiagnosticPositionTrackerConstructor ctor) {
      this.ctor = ctor;
    }

    public DiagnosticPositionTracker get(SrcFilePair srcFilePair, TrackersSharedState sharedState) {
      if (tracker == null) {
        try {
          tracker = ctor.create(srcFilePair, sharedState);
        } catch (DiffException | IOException e) {
          throw new RuntimeException(e);
        }
      }

      return tracker;
    }
  }
}
