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
import com.google.errorprone.bugtrack.motion.LineMotionTracker;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

@RunWith(JUnit4.class)
public class LineMotionTrackerTest {
    private static final String TEST_REPO = "src/test/java/com/google/errorprone/bugtrack/testdata/";

    private List<String> loadTestFile(String file) throws IOException {
        return Files.readAllLines(Paths.get(TEST_REPO + file));
    }

    @Test
    public void canTrackSingleLineMove() throws IOException, DiffException {
        List<String> oldFile = loadTestFile("foo_1");
        List<String> newFile = loadTestFile("foo_2");

        LineMotionTracker<String> lineMotionTracker = new LineMotionTracker<>(oldFile, newFile);

        Assert.assertEquals(Optional.of(5L), lineMotionTracker.getNewLine(4));
    }

    @Test
    public void canTrackLargerFileChange() throws IOException, DiffException {
        List<String> oldFile = loadTestFile("foo_2");
        List<String> newFile = loadTestFile("foo_4");

        LineMotionTracker<String> lineMotionTracker = new LineMotionTracker<>(oldFile, newFile);

        Assert.assertEquals(Optional.of(1L), lineMotionTracker.getNewLine(1));
        Assert.assertEquals(Optional.of(7L), lineMotionTracker.getNewLine(3));
        Assert.assertEquals(Optional.empty(), lineMotionTracker.getNewLine(4));
        Assert.assertEquals(Optional.empty(), lineMotionTracker.getNewLine(5));
        Assert.assertEquals(Optional.of(12L), lineMotionTracker.getNewLine(6));
    }

    @Test
    public void canTrackRealLifeDiffChange() throws IOException, DiffException {
        List<String> oldFile = loadTestFile("Tag");
        List<String> newFile = loadTestFile("Tag_Newer");

        LineMotionTracker<String> lineMotionTracker = new LineMotionTracker<>(oldFile, newFile);

        Assert.assertEquals(Optional.of(20L), lineMotionTracker.getNewLine(18));
        Assert.assertEquals(Optional.of(17L), lineMotionTracker.getNewLine(16));
        Assert.assertEquals(Optional.of(30L), lineMotionTracker.getNewLine(29));
    }

    @Test
    public void canTrackLinesAroundADeletion() throws IOException, DiffException {
        List<String> oldFile = loadTestFile("Tag");
        List<String> newFile = loadTestFile("Tag_Newer");

        LineMotionTracker<String> lineMotionTracker = new LineMotionTracker<>(oldFile, newFile);

        Assert.assertEquals(Optional.of(20L), lineMotionTracker.getNewLine(18));
        Assert.assertEquals(Optional.empty(), lineMotionTracker.getNewLine(19));
        Assert.assertEquals(Optional.empty(), lineMotionTracker.getNewLine(20));
        Assert.assertEquals(Optional.of(21L), lineMotionTracker.getNewLine(21));
    }

    @Test
    public void canTrackLinesAroundAnInsertion() throws IOException, DiffException {
        List<String> oldFile = loadTestFile("Tag");
        List<String> newFile = loadTestFile("Tag_Newer");

        LineMotionTracker<String> lineMotionTracker = new LineMotionTracker<>(oldFile, newFile);

        Assert.assertEquals(Optional.of(232L), lineMotionTracker.getNewLine(221));
        Assert.assertEquals(Optional.of(221L), lineMotionTracker.getNewLine(219));
        Assert.assertEquals(Optional.of(222L), lineMotionTracker.getNewLine(220));
    }
}
