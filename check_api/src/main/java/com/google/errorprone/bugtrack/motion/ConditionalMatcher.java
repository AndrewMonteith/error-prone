package com.google.errorprone.bugtrack.motion;

import com.google.errorprone.bugtrack.*;
import com.google.errorprone.bugtrack.motion.trackers.DiagnosticPredicates;

import java.io.IOException;

public final class ConditionalMatcher implements BugComparer {
  private final SrcPairInfo srcPairInfo;
  private final DiagnosticPredicates.Predicate diagPredicate;
  private final Lazy<BugComparer> comparerIfTrue;
  private final Lazy<BugComparer> comparerIfFalse;

  public ConditionalMatcher(
      SrcPairInfo srcPairInfo,
      DiagnosticPredicates.Predicate diagPredicate,
      BugComparerCtor comparerIfTrueCtor,
      BugComparerCtor comparerIfFalseCtor) {
    this.srcPairInfo = srcPairInfo;
    this.diagPredicate = diagPredicate;
    this.comparerIfTrue = new Lazy<>(() -> comparerIfTrueCtor.get(srcPairInfo));
    this.comparerIfFalse = new Lazy<>(() -> comparerIfFalseCtor.get(srcPairInfo));
  }

  @Override
  public boolean areSame(DatasetDiagnostic oldDiagnostic, DatasetDiagnostic newDiagnostic)
      throws IOException {
    if (!oldDiagnostic.isSameType(newDiagnostic)) {
      return false;
    }

    BugComparer comparer =
        diagPredicate.test(srcPairInfo.files, oldDiagnostic, newDiagnostic)
            ? comparerIfTrue.get()
            : comparerIfFalse.get();

    return comparer.areSame(oldDiagnostic, newDiagnostic);
  }
}
