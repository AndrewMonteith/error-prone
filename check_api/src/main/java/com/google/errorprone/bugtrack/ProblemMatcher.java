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
    return modifyMessage(oldDiagnostic).equals(modifyMessage(newDiagnostic));
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
        return sortMessage(diagnostic.getMessageWithoutFix());
      case "UngroupedOverloads":
        return ""; // encodes syntatic information in message so should omit it
      default:
        return diagnostic.getMessageWithoutFix();
    }
  }
}
