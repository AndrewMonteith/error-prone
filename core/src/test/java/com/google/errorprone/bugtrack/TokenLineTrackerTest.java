/*
 * Copyright 2020 The Error Prone Authors.
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
import com.google.errorprone.bugtrack.motion.SrcFile;
import com.google.errorprone.bugtrack.motion.SrcFilePair;
import com.google.errorprone.bugtrack.motion.trackers.DPTrackerConstructorFactory;
import com.google.errorprone.bugtrack.motion.trackers.DiagnosticPositionTracker;
import com.google.errorprone.bugtrack.motion.trackers.TrackersSharedState;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import static com.google.errorprone.bugtrack.TestUtils.readTestFile;

@RunWith(JUnit4.class)
public class TokenLineTrackerTest {
    private void assertLineIsTracked(DiagnosticPositionTracker lineMotionTracker, final long oldLine, final long newLine) {
        DatasetDiagnostic mockOldDiag = new DatasetDiagnostic("", oldLine, 1, "");
        Assert.assertEquals(newLine, lineMotionTracker.track(mockOldDiag).get().getLine());
    }

    private void assertLineIsNotTracked(DiagnosticPositionTracker lineMotionTracker, final long oldLine) {
        DatasetDiagnostic mockOldDiag = new DatasetDiagnostic("", oldLine, 1, "");
        Assert.assertEquals(Optional.empty(), lineMotionTracker.track(mockOldDiag));
    }

    private void performTest(String oldFileName, String newFileName, Consumer<DiagnosticPositionTracker> test) throws IOException, DiffException {
        List<String> oldFileSrc = readTestFile(oldFileName);
        List<String> newFileSrc = readTestFile(newFileName);

        SrcFile oldFile = new SrcFile(oldFileName, oldFileSrc);
        SrcFile newFile = new SrcFile(newFileName, newFileSrc);

        DiagnosticPositionTracker positionTracker = DPTrackerConstructorFactory
                .newTokenizedLineTracker().create(
                        new SrcFilePair(oldFile, newFile),
                        new TrackersSharedState());

        test.accept(positionTracker);
    }

    @Test
    public void canTrackSingleLineMove() throws IOException, DiffException {
        performTest("foo_1.java", "foo_2.java", lineMotionTracker -> {
            assertLineIsTracked(lineMotionTracker, 3, 4);
        });
    }

    @Test
    public void canTrackLargerFileChanges() throws IOException, DiffException {
        performTest("foo_2.java", "foo_4.java", lineMotionTracker -> {
            assertLineIsTracked(lineMotionTracker, 1, 1);
            assertLineIsTracked(lineMotionTracker, 2, 6);
            assertLineIsNotTracked(lineMotionTracker, 3);
            assertLineIsTracked(lineMotionTracker, 5, 11);
        });
    }

    @Test
    public void canTrackLinesAroundADeletion() throws IOException, DiffException {
        performTest("Tag.java", "Tag_Newer.java", lineMotionTracker -> {
            assertLineIsTracked(lineMotionTracker, 18,20);
            assertLineIsNotTracked(lineMotionTracker, 19);
            assertLineIsNotTracked(lineMotionTracker, 20);
            assertLineIsTracked(lineMotionTracker, 21,21);
        });
    }

    @Test
    public void canTrackLinesAroundAnInsertion() throws IOException, DiffException {
        performTest("Tag.java", "Tag_Newer.java", lineMotionTracker -> {
            assertLineIsTracked(lineMotionTracker, 18,20);
            assertLineIsNotTracked(lineMotionTracker, 19);
            assertLineIsNotTracked(lineMotionTracker, 20);
            assertLineIsTracked(lineMotionTracker, 21,21);
        });
    }

    @Test
    public void isInsensitiveToWhitespace() throws IOException, DiffException {
        performTest("foo_2.java", "foo_4_indented.java", lineMotionTracker -> {
            assertLineIsTracked(lineMotionTracker, 1, 1);
            assertLineIsTracked(lineMotionTracker, 2, 6);
            assertLineIsNotTracked(lineMotionTracker, 3);
            assertLineIsTracked(lineMotionTracker, 5, 11);
            assertLineIsTracked(lineMotionTracker, 4, 7);
        });
    }

    @Test
    public void canTrackIndentation() throws IOException, DiffException {
        performTest("foo_2.java", "foo_4_indented.java", lineMotionTracker -> {
            // GIVEN:
            DatasetDiagnostic mockDiag = new DatasetDiagnostic("", 1, 1, null);

            // WHEN:
            Optional<DiagPosEqualityOracle> newPos = lineMotionTracker.track(mockDiag);

            // THEN:
            Assert.assertEquals(Optional.of(DiagPosEqualityOracle.byLineCol(1, 5)), newPos);
        });
    }
}
