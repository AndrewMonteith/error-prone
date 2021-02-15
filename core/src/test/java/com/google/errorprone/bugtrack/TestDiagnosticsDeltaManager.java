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

import com.google.errorprone.bugtrack.motion.DiagnosticsDeltaManager;
import com.google.errorprone.bugtrack.motion.SrcFile;

import java.io.IOException;

public class TestDiagnosticsDeltaManager implements DiagnosticsDeltaManager {
    @Override
    public boolean inSameFile(DatasetDiagnostic oldDiagnostic, DatasetDiagnostic newDiagnostic) {
        return true;
    }

    @Override
    public SrcFilePair loadFilesBetweenDiagnostics(DatasetDiagnostic oldDiagnostic, DatasetDiagnostic newDiagnostic) throws IOException {
        return new SrcFilePair(
                TestUtils.readTestSrcFile(oldDiagnostic.getFileName()),
                TestUtils.readTestSrcFile(newDiagnostic.getFileName()));
    }
}
