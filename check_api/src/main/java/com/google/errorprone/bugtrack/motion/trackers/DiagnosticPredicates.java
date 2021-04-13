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
   * in ..." and "add implements method in ..." even though they're the same method. Another example
   * is MissingCasesInEnumSwitch who's error message can be "missing cases 1, 2, 3 and N others"
   * others. That N depends on an enum defined in another file so that message can change.
   */
  private static final ImmutableSet<String> DESCRIPTIONS_CAN_SYNTACTICALLY_CHANGE =
      ImmutableSet.of(
          "MissingSummary",
          "FunctionalInterfaceClash",
          "MissingCasesInEnumSwitch",
          "MissingOverride");

  public static Predicate cannotTrackEndpoints() {
    return (srcFilePair, oldDiag, newDiag) -> {
      if (MULTIPLE_IN_SAME_REGION.contains(oldDiag.getType())) {
        return srcFilePair.oldFile.testSubstring(oldDiag, letter -> letter == ',');
      } else {
        return false;
      }
    };
  }

  public static Predicate canTrackIdentically() {
    return (srcFilePair, oldDiag, newDiag) -> {
      if ((oldDiag.getEndPos() == -1 && newDiag.getEndPos() != -1)
          || (oldDiag.getEndPos() != -1 && newDiag.getEndPos() == -1)) {
        // Edge case when collecting where sometimes the end diagnostics would have a random '-1'
        // even though the prior commit we managed to get the end positioon
        return false;
      }

      return !(srcFilePair.srcChanged
          || DESCRIPTIONS_CAN_SYNTACTICALLY_CHANGE.contains(oldDiag.getType()));
    };
  }

  @FunctionalInterface
  public interface Predicate {
    boolean test(
        SrcFilePair srcFilePair, DatasetDiagnostic oldDiagnostic, DatasetDiagnostic newDiagnostic);
  }
}
