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

package com.google.errorprone.bugtrack.motion;

import com.google.errorprone.bugtrack.BugComparer;
import com.google.errorprone.bugtrack.DatasetDiagnostic;

public class ExactDiagnosticMatcher implements BugComparer {
  @Override
  public boolean areSame(DatasetDiagnostic oldDiagnostic, DatasetDiagnostic newDiagnostic) {
    if (oldDiagnostic.getLineNumber() == -1 || newDiagnostic.getLineNumber() == -1) {
      return false;
    }

    if (!oldDiagnostic.isSameType(newDiagnostic)) {
      return false;
    }

    // We don't use .equals since that accounts for the same file name but the file could have
    // been renamed since matching guarentees we only match diagnostics in the same file then we
    // need only compare everything else for equality
    return oldDiagnostic.refersToSameSource(newDiagnostic);
  }
}
