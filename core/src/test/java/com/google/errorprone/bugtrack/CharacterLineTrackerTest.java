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
import com.google.errorprone.bugtrack.motion.DiagPosEqualityOracle;
import com.google.errorprone.bugtrack.motion.trackers.CharacterLineTracker;
import com.google.errorprone.bugtrack.motion.trackers.DiagnosticPositionTracker;
import org.junit.Assert;
import org.junit.Test;
import org.openjdk.tools.javac.util.JCDiagnostic;

import javax.tools.Diagnostic;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

import static com.google.errorprone.bugtrack.TestUtils.readTestFile;

public final class CharacterLineTrackerTest {
    private void assertLineIsTracked(DiagnosticPositionTracker lineMotionTracker, final long oldLine, final long newLine) {
        DatasetDiagnostic mockOldDiag = new DatasetDiagnostic("", oldLine, 1, "");
        Assert.assertEquals(newLine, lineMotionTracker.track(mockOldDiag).get().getLine());
    }

    private void assertLineIsNotTracked(DiagnosticPositionTracker lineMotionTracker, final long oldLine) {
        DatasetDiagnostic mockOldDiag = new DatasetDiagnostic("", oldLine, 1, "");
        Assert.assertEquals(Optional.empty(), lineMotionTracker.track(mockOldDiag));
    }

    @Test
    public void canTrackSingleLineMove() throws IOException, DiffException {
        List<String> oldSrc = readTestFile("foo_1.java");
        List<String> newSrc = readTestFile("foo_2.java");

        DiagnosticPositionTracker positionTracker = new CharacterLineTracker(oldSrc, newSrc);

        assertLineIsTracked(positionTracker, 3, 4);
    }

    @Test
    public void canTrackLargerFileChange() throws IOException, DiffException {
        List<String> oldSrc = readTestFile("foo_2.java");
        List<String> newSrc = readTestFile("foo_4.java");

        DiagnosticPositionTracker positionTracker = new CharacterLineTracker(oldSrc, newSrc);

        assertLineIsTracked(positionTracker, 1, 1);
        assertLineIsTracked(positionTracker, 2, 6);
        assertLineIsNotTracked(positionTracker, 3);
        assertLineIsTracked(positionTracker, 6, 12);
    }

    @Test
    public void canTrackRealLifeDiffChange() throws IOException, DiffException {
        List<String> oldSrc = readTestFile("Tag.java");
        List<String> newSrc = readTestFile("Tag_Newer.java");

        DiagnosticPositionTracker positionTracker = new CharacterLineTracker(oldSrc, newSrc);

        assertLineIsTracked(positionTracker, 18, 20);
        assertLineIsTracked(positionTracker, 16, 17);
        assertLineIsTracked(positionTracker, 29, 30);
    }

    @Test
    public void canTrackLinesAroundADeletion() throws IOException, DiffException {
        List<String> oldSrc = readTestFile("Tag.java");
        List<String> newSrc = readTestFile("Tag_Newer.java");

        DiagnosticPositionTracker positionTracker = new CharacterLineTracker(oldSrc, newSrc);

        assertLineIsTracked(positionTracker, 18, 20);
        assertLineIsNotTracked(positionTracker, 19);
        assertLineIsNotTracked(positionTracker, 20);
        assertLineIsTracked(positionTracker, 21, 21);
    }

    @Test
    public void canTrackLinesAroundAnInsertion() throws IOException, DiffException {
        List<String> oldSrc = readTestFile("Tag.java");
        List<String> newSrc = readTestFile("Tag_Newer.java");

        DiagnosticPositionTracker positionTracker = new CharacterLineTracker(oldSrc, newSrc);

        assertLineIsTracked(positionTracker, 221, 232);
        assertLineIsTracked(positionTracker, 219, 221);
        assertLineIsTracked(positionTracker, 220, 222);
    }
}
