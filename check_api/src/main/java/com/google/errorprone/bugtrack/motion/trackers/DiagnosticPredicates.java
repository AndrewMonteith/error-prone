package com.google.errorprone.bugtrack.motion.trackers;

import com.google.common.collect.ImmutableSet;
import com.google.errorprone.bugtrack.DatasetDiagnostic;
import com.google.errorprone.bugtrack.motion.SrcFile;

public class DiagnosticPredicates {
  /**
   * Any diagnostic type which can spawn many instances for the same [start-pos, end-pos] region and
   * hence must be discern by it's specific position. Essentially this is a list of diagnostics
   * unsuitable for being track by start and end pos
   */
  private static final ImmutableSet<String> MULTIPLE_PER_LIME =
      ImmutableSet.of(
          "MultiVariableDeclaration", "FieldCanBeFinal", "InitializeInline", "MemberName");

  public static Predicate manyInSameRegion() {
    return (srcFile, diag) -> {
      if (MULTIPLE_PER_LIME.contains(diag.getType())) {
        return srcFile.getSrcExtract(diag).contains(",");
      } else {
        return false;
      }
    };
  }

  @FunctionalInterface
  public interface Predicate {
    boolean test(SrcFile file, DatasetDiagnostic diagnostic);
  }
}
