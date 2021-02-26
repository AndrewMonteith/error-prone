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

package com.google.errorprone.bugtrack.harness.evaluating;

import com.google.errorprone.bugtrack.harness.evaluating.MissedLikelihoodCalculator;

public final class MissedLikelihoodCalculatorFactory {
    private MissedLikelihoodCalculatorFactory() {
    }

    /*
     * Largest substring of 'str2' contained in 'str1' / len of 'str2'
     */
    private static double stringIntersection(String str1, String str2) {
        double maxLen = 0;
        for (int i = 0; i < str2.length(); ++i) {
            for (int j = i; j < str2.length(); ++j) {
                if (str1.contains(str2.substring(i, j))) {
                    maxLen = Math.max(maxLen, j - i + 1);
                }
            }
        }

        return maxLen / str2.length();
    }

    public static MissedLikelihoodCalculator diagSrcOverlap() {
        return (srcFilePair, oldDiagnostic, newDiagnostic) -> {
            if (oldDiagnostic.getLineNumber() == -1 || newDiagnostic.getLineNumber() == -1) {
                return oldDiagnostic.getLineNumber() == newDiagnostic.getLineNumber() ? 1 : 0;
            }

            return stringIntersection(
                    srcFilePair.oldFile.getSrcExtract(oldDiagnostic),
                    srcFilePair.newFile.getSrcExtract(newDiagnostic));
        };
    }

    public static MissedLikelihoodCalculator diagLineSrcOverlap() {
        return (srcFilePair, oldDiagnostic, newDiagnostic) -> {
            if (oldDiagnostic.getLineNumber() == -1 || newDiagnostic.getLineNumber() == -1) {
                return oldDiagnostic.getLineNumber() == newDiagnostic.getLineNumber() ? 1 : 0;
            }

            return stringIntersection(
                    srcFilePair.oldFile.getLines().get((int) (oldDiagnostic.getLineNumber() - 1)),
                    srcFilePair.newFile.getLines().get((int) (newDiagnostic.getLineNumber() - 1)));
        };
    }

    public static MissedLikelihoodCalculator lineDistance() {
        return (srcFilePair, oldDiag, newDiag) ->
                1.0 / (1 + oldDiag.getLineNumber() - newDiag.getLineNumber());
    }

    public static MissedLikelihoodCalculator average(MissedLikelihoodCalculator calc1, MissedLikelihoodCalculator calc2) {
        return (srcFilePair, oldDiag, newDiag) ->
                0.5 * (calc1.compute(srcFilePair, oldDiag, newDiag) + calc2.compute(srcFilePair, oldDiag, newDiag));
    }

    public static MissedLikelihoodCalculator weighted(float w1,
                                                      MissedLikelihoodCalculator calc1,
                                                      float w2,
                                                      MissedLikelihoodCalculator calc2) {
        return (srcFilePair, oldDiag, newDiag) ->
                (w1 * calc1.compute(srcFilePair, oldDiag, newDiag)) + (w2 * calc2.compute(srcFilePair, oldDiag, newDiag)) / (w1 + w2);
    }

    public static MissedLikelihoodCalculator zero() {
        return (srcFilePair, oldDiagnostic, newDiagnostic) -> 0;
    }
}
