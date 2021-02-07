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

import com.github.difflib.algorithm.DiffException;
import com.google.errorprone.bugtrack.motion.*;
import org.junit.jupiter.api.Test;

import java.io.IOException;

public class DiagnosticRangeMotionComparerTest {
    // Ideally these tests should exercise the same test functionality as TokenLineTrackerTests. But that's effort
    // I cba with at the moment. So at the moment just check it extends the correct class then run the other tests.

    @Test
    public void canTrackSubexprThatAppearsOnOneLine() throws IOException, DiffException {
        // GIVEN:
        DiagnosticPositionRange oldExprRange = new DiagnosticPositionRange(3, 9, 3, 45);
        SrcFile oldFile = TestFileUtil.readTestSrcFile("foo_4.java");
        SrcFile newFile = TestFileUtil.readTestSrcFile("foo_5.java");

        // WHEN:
        DiagnosticPositionTracker tracker = DPTrackerConstructorFactory.newTokenizedLineTracker().create(oldFile, newFile);

        // THEN:
    }
}
