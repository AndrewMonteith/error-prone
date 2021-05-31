package com.google.errorprone.bugtrack.motion.trackers;

import com.google.common.collect.ImmutableSet;
import com.google.errorprone.bugtrack.DatasetDiagnostic;
import com.google.errorprone.bugtrack.motion.SrcFilePair;

public class DiagnosticPredicates {

  /** Diagnostic who's better at line tracking than IJM Tracking */
  public static ImmutableSet<String> BETTER_WITH_LINE =
      ImmutableSet.of(
          "[WildcardImport]",
          "[InterruptedExceptionSwallowed]",
          "[MissingCasesInEnumSwitch]",
          "[FallThrough]",
          "[UnnecessaryDefaultInEnumSwitch]",
          "[MissingDefault]",
          "[SuppressWarningsWithoutExplanation]",
          "[UnusedException]");

  public static Predicate canTrackIdenticalLocation() {
    return (srcFilePair, oldDiag, newDiag) -> !srcFilePair.srcChanged;
  }

  public static Predicate betterWithLineTracking() {
    return (srcFilePair, oldDiag, newDiag) -> BETTER_WITH_LINE.contains(oldDiag.getType());
  }

  @FunctionalInterface
  public interface Predicate {
    boolean test(
        SrcFilePair srcFilePair, DatasetDiagnostic oldDiagnostic, DatasetDiagnostic newDiagnostic);
  }
}
