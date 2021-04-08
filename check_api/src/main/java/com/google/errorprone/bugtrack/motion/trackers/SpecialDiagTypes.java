package com.google.errorprone.bugtrack.motion.trackers;

import com.google.common.collect.ImmutableSet;

public class SpecialDiagTypes {

  /**
   * Any diagnostic type which can spawn many instances for the same [start-pos, end-pos] region and
   * hence must be discern by it's specific position. Essentially this is a list of diagnostics
   * unsuitable for being track by start and end pos
   */
  public static ImmutableSet<String> MULTIPLE_PER_LIME =
      ImmutableSet.of("MultiVariableDeclaration", "FieldCanBeFinal");
}
