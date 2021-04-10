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
import com.google.errorprone.bugtrack.motion.ExactDiagnosticMatcher;
import com.google.errorprone.bugtrack.motion.trackers.DiagnosticPositionTrackerConstructor;
import com.google.errorprone.bugtrack.utils.ThrowingFunction;
import com.google.errorprone.bugtrack.utils.ThrowingPredicate;

import java.util.Arrays;
import java.util.List;
import java.util.function.BiFunction;

public final class BugComparers {
  public static BugComparerCtor trackIdentical() {
    return srcPairInfo -> new ExactDiagnosticMatcher();
  }

  public static BugComparerCtor trackPosition(DiagnosticPositionTrackerConstructor constructor) {
    return srcPairInfo -> new DiagPosMatcher(srcPairInfo, constructor.create(srcPairInfo));
  }

  public static BugComparerCtor conditional(
      BiFunction<SrcPairInfo, DatasetDiagnostic, Boolean> predicate,
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
}
