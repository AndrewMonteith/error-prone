package com.google.errorprone.bugtrack.motion.trackers;

import com.google.common.collect.ImmutableSet;
import com.google.errorprone.bugtrack.DatasetDiagnostic;
import com.google.errorprone.bugtrack.motion.SrcFilePair;

public class DiagnosticPredicates {
  /**
   * Any diagnostic type which can spawn many instances for the same [start-pos, end-pos] region and
   * hence must be discern by it's specific position. Essentially this is a list of diagnostics
   * unsuitable for being track by start and end pos
   */
  private static final ImmutableSet<String> MULTIPLE_IN_SAME_REGION =
      ImmutableSet.of(
          "MultiVariableDeclaration",
          "FieldCanBeFinal",
          "InitializeInline",
          "MemberName",
          "UnusedVariable");

  /**
   * Diagnostics who's description can change however we would still want to track the two
   * individual diagnostics with different descriptions. An example of this is MissingSummary which
   * for subtle reasons may confuse where the original method declaration you're override is (where
   * it's in an interface or class), so it's description may alternate between "add overrides method
   * in ..." and "add implements method in ..." even though they're the same method.
   */
  private static final ImmutableSet<String> DESCRIPTIONS_CAN_SYNTACTICALLY_CHANGE =
      ImmutableSet.of("MissingSummary", "FunctionalInterfaceClash");

  public static Predicate manyInSameRegion() {
    return (srcFilePair, diag) -> {
      if (MULTIPLE_IN_SAME_REGION.contains(diag.getType())) {
        return srcFilePair.oldFile.testSubstring(diag, letter -> letter == ',');
      } else {
        return false;
      }
    };
  }

  public static Predicate canTrackIdentically() {
    return (srcFilePair, diag) ->
        !(srcFilePair.srcChanged || diag.getType().equals("MissingSummary"));
  }

  @FunctionalInterface
  public interface Predicate {
    boolean test(SrcFilePair srcFilePair, DatasetDiagnostic diagnostic);
  }
}
