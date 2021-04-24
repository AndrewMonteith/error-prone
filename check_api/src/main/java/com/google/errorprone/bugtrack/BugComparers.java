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

package com.google.errorprone.bugtrack;

import com.google.common.collect.ImmutableList;
import com.google.errorprone.bugtrack.motion.ConditionalMatcher;
import com.google.errorprone.bugtrack.motion.DiagPosMatcher;
import com.google.errorprone.bugtrack.motion.ExactLocationMatcher;
import com.google.errorprone.bugtrack.motion.trackers.DiagnosticPositionTrackerConstructor;
import com.google.errorprone.bugtrack.motion.trackers.DiagnosticPositionTrackers;
import com.google.errorprone.bugtrack.motion.trackers.DiagnosticPredicates;
import com.google.errorprone.bugtrack.utils.ThrowingFunction;
import com.google.errorprone.bugtrack.utils.ThrowingPredicate;

import java.util.Arrays;
import java.util.List;

import static com.google.errorprone.bugtrack.motion.trackers.DiagnosticPositionTrackers.newIJMPosTracker;
import static com.google.errorprone.bugtrack.motion.trackers.DiagnosticPositionTrackers.newIJMStartAndEndTracker;

public final class BugComparers {
  //  public static final BugComparerCtor DEFAULT_COMPARER2 =
  //      BugComparers.conditional(
  //          DiagnosticPredicates.canTrackIdentically(),
  //          trackIdentical(),
  //          trackPosition(
  //              DiagnosticPositionTrackers.any(newIJMStartAndEndTracker(), newIJMPosTracker())));

  public static final BugComparerCtor DEFAULT_COMPARER =
      and(
          matchProblem(),
          conditional(
              DiagnosticPredicates.canTrackIdenticalLocation(),
              matchIdenticalLocation(),
              trackPosition(
                  DiagnosticPositionTrackers.any(newIJMStartAndEndTracker(), newIJMPosTracker()))));

  public static BugComparerCtor matchProblem() {
    return srcPairInfo -> new ProblemMatcher();
  }

  public static BugComparerCtor matchIdenticalLocation() {
    return srcPairInfo -> new ExactLocationMatcher();
  }

  public static BugComparerCtor trackPosition(DiagnosticPositionTrackerConstructor constructor) {
    return srcPairInfo -> new DiagPosMatcher(srcPairInfo, constructor.create(srcPairInfo));
  }

  public static BugComparerCtor conditional(
      DiagnosticPredicates.Predicate predicate,
      BugComparerCtor comparerIfTrue,
      BugComparerCtor comparerIfFalse) {
    return srcPairInfo ->
        new ConditionalMatcher(srcPairInfo, predicate, comparerIfTrue, comparerIfFalse);
  }

  public static BugComparerCtor any(BugComparerCtor... ctors) {
    return srcPairInfo -> {
      List<BugComparer> comparers =
          Arrays.stream(ctors)
              .map((ThrowingFunction<BugComparerCtor, BugComparer>) ctor -> ctor.get(srcPairInfo))
              .collect(ImmutableList.toImmutableList());

      return (oldDiag, newDiag) ->
          comparers.stream()
              .anyMatch(
                  (ThrowingPredicate<BugComparer>) comparer -> comparer.areSame(oldDiag, newDiag));
    };
  }

  public static BugComparerCtor and(BugComparerCtor... ctors) {
    return srcPairInfo -> {
      List<BugComparer> comparers =
          Arrays.stream(ctors)
              .map((ThrowingFunction<BugComparerCtor, BugComparer>) ctor -> ctor.get(srcPairInfo))
              .collect(ImmutableList.toImmutableList());

      return (oldDiag, newDiag) ->
          comparers.stream()
              .allMatch(
                  (ThrowingPredicate<BugComparer>) comparer -> comparer.areSame(oldDiag, newDiag));
    };
  }
}
