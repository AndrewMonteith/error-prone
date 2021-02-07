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
import com.google.errorprone.bugtrack.motion.CharacterLineTracker;
import com.google.errorprone.bugtrack.motion.DiagnosticPosition;
import com.google.errorprone.bugtrack.motion.DiagnosticPositionTracker;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

public final class CharacterLineTrackerTest {
    private static final String TEST_REPO = "src/test/java/com/google/errorprone/bugtrack/testdata/";

    private List<String> loadTestFile(String file) throws IOException {
        return Files.readAllLines(Paths.get(TEST_REPO + file));
    }

    private void assertLineIsTracked(DiagnosticPositionTracker lineMotionTracker, final long oldLine, Optional<Long> expectedNewLine) {
        Optional<Long> newLine = lineMotionTracker.getNewPosition(oldLine, 1).map(position -> position.line);

        Assert.assertEquals(expectedNewLine, newLine);
    }

    @Test
    public void canTrackSingleLineMove() throws IOException, DiffException {
        List<String> oldSrc = loadTestFile("foo_1.java");
        List<String> newSrc = loadTestFile("foo_2.java");

        DiagnosticPositionTracker positionTracker = new CharacterLineTracker(oldSrc, newSrc);

        assertLineIsTracked(positionTracker, 3, Optional.of(4L));
    }

    @Test
    public void canTrackLargerFileChange() throws IOException, DiffException {
        List<String> oldSrc = loadTestFile("foo_2.java");
        List<String> newSrc = loadTestFile("foo_4.java");

        DiagnosticPositionTracker positionTracker = new CharacterLineTracker(oldSrc, newSrc);

        assertLineIsTracked(positionTracker, 1, Optional.of(1L));
        assertLineIsTracked(positionTracker, 2, Optional.of(6L));
        assertLineIsTracked(positionTracker, 3, Optional.empty());
        assertLineIsTracked(positionTracker, 6, Optional.of(12L));
    }

    @Test
    public void canTrackRealLifeDiffChange() throws IOException, DiffException {
        List<String> oldSrc = loadTestFile("Tag.java");
        List<String> newSrc = loadTestFile("Tag_Newer.java");

        DiagnosticPositionTracker positionTracker = new CharacterLineTracker(oldSrc, newSrc);

        assertLineIsTracked(positionTracker, 18, Optional.of(20L));
        assertLineIsTracked(positionTracker, 16, Optional.of(17L));
        assertLineIsTracked(positionTracker, 29, Optional.of(30L));
    }

    @Test
    public void canTrackLinesAroundADeletion() throws IOException, DiffException {
        List<String> oldSrc = loadTestFile("Tag.java");
        List<String> newSrc = loadTestFile("Tag_Newer.java");

        DiagnosticPositionTracker positionTracker = new CharacterLineTracker(oldSrc, newSrc);

        assertLineIsTracked(positionTracker, 18, Optional.of(20L));
        assertLineIsTracked(positionTracker, 19, Optional.empty());
        assertLineIsTracked(positionTracker, 20, Optional.empty());
        assertLineIsTracked(positionTracker, 21, Optional.of(21L));
    }

    @Test
    public void canTrackLinesAroundAnInsertion() throws IOException, DiffException {
        List<String> oldSrc = loadTestFile("Tag.java");
        List<String> newSrc = loadTestFile("Tag_Newer.java");

        DiagnosticPositionTracker positionTracker = new CharacterLineTracker(oldSrc, newSrc);

        assertLineIsTracked(positionTracker, 221, Optional.of(232L));
        assertLineIsTracked(positionTracker, 219, Optional.of(221L));
        assertLineIsTracked(positionTracker, 220, Optional.of(222L));
    }
}
