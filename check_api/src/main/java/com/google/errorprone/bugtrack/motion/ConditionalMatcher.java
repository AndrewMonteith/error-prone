package com.google.errorprone.bugtrack.motion;

import com.google.errorprone.bugtrack.*;

import java.io.IOException;
import java.util.function.BiFunction;

public final class ConditionalMatcher implements BugComparer {
  private final SrcPairInfo srcPairInfo;
  private final BiFunction<SrcPairInfo, DatasetDiagnostic, Boolean> diagPredicate;
  private Lazy<BugComparer> comparerIfTrue;
  private Lazy<BugComparer> comparerIfFalse;

  public ConditionalMatcher(
      SrcPairInfo srcPairInfo,
      BiFunction<SrcPairInfo, DatasetDiagnostic, Boolean> diagPredicate,
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
    BugComparer comparer =
        diagPredicate.apply(srcPairInfo, oldDiagnostic)
            ? comparerIfTrue.get()
            : comparerIfFalse.get();

    return comparer.areSame(oldDiagnostic, newDiagnostic);
  }
}
