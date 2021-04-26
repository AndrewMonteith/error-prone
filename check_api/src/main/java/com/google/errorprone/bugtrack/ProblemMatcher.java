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

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;

import java.io.IOException;
import java.util.stream.StreamSupport;

public final class ProblemMatcher implements BugComparer {

  @Override
  public boolean areSame(DatasetDiagnostic oldDiagnostic, DatasetDiagnostic newDiagnostic)
      throws IOException {
    if (oldDiagnostic.getType().equals("MissingOverride")) {
      return handleMissingOverrideEdgeCase(oldDiagnostic, newDiagnostic);
    } else {
      return modifyMessage(oldDiagnostic).equals(modifyMessage(newDiagnostic));
    }
  }

  private static boolean handleMissingOverrideEdgeCase(
      DatasetDiagnostic oldDiag, DatasetDiagnostic newDiag) {
    String oldMsg = oldDiag.getBriefMessage();
    String newMsg = newDiag.getBriefMessage();

    // For weird external reasons diagnostics may alternate between thinking a method belongs to
    // either AbstractCollection or Set. In the first case this means overriding a method, in the
    // section implementing an interface method. This makes the diagnostic message change it's
    // format
    // but in reality they are the same problem, just affected by external factors
    if ((oldMsg.contains("AbstractCollection") && newMsg.contains("Set"))
        || oldMsg.contains("Set") && newMsg.contains("AbstractCollection")) {
      String oldMethodName = extractMethodNameFromMissingOverride(oldMsg);
      String newMethodName = extractMethodNameFromMissingOverride(newMsg);

      return oldMethodName.equals(newMethodName);
    } else {
      return oldMsg.equals(newMsg);
    }
  }

  private static String extractMethodNameFromMissingOverride(String missingSummary) {
    final int firstSpace = missingSummary.indexOf(' ');
    final int secondSpace = missingSummary.indexOf(' ', firstSpace + 1);

    return missingSummary.substring(firstSpace, secondSpace);
  }

  private static final Joiner JOIN_ON_LINES = Joiner.on('\n');
  private static final Splitter SPLIT_ON_LINES = Splitter.on('\n');

  private static String sortMessage(String message) {
    return JOIN_ON_LINES.join(
        StreamSupport.stream(SPLIT_ON_LINES.split(message).spliterator(), false)
            .sorted()
            .iterator());
  }

  private static String modifyMessage(DatasetDiagnostic diagnostic) {
    switch (diagnostic.getType()) {
      case "FunctionalInterfaceClash":
        return sortMessage(diagnostic.getBriefMessage());
      case "UngroupedOverloads":
        return ""; // encodes syntatic information in message so should omit it
      default:
        return diagnostic.getBriefMessage();
    }
  }
}
