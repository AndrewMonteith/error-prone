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
import com.google.common.collect.ImmutableList;
import com.google.errorprone.ErrorProneInMemoryFileManager;
import com.google.errorprone.bugtrack.motion.*;
import com.sun.tools.javac.api.JavacTaskImpl;
import com.sun.tools.javac.api.JavacTool;
import com.sun.tools.javac.util.Context;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import javax.tools.JavaCompiler;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import static java.nio.charset.StandardCharsets.UTF_8;

@RunWith(JUnit4.class)
public class TokenLineTrackerTest {
    private static final String TEST_REPO = "src/test/java/com/google/errorprone/bugtrack/testdata/";

    private static List<String> readTestFile(String file) throws IOException {
        return Files.readAllLines(Paths.get(TEST_REPO + file));
    }

    private final ErrorProneInMemoryFileManager fileManager = new ErrorProneInMemoryFileManager();

    private Context getContextForFile(String testFile) {
        JavaCompiler javaCompiler = JavacTool.create();
        JavacTaskImpl task =
                (JavacTaskImpl)
                        javaCompiler.getTask(
                                new PrintWriter(
                                        new BufferedWriter(new OutputStreamWriter(System.err, UTF_8)), true),
                                fileManager,
                                null,
                                Collections.emptyList(),
                                null,
                                ImmutableList.of(fileManager.getJavaFileObject(Paths.get(TEST_REPO, testFile))));

        task.parse();
//       Not necessary?:
//        task.analyze();

        return task.getContext();
    }

    private void assertLineNumberIsTracked(DiagnosticPositionTracker lineMotionTracker, final long oldLine, Optional<Long> expectedNewLine) {
        Optional<Long> newLine = lineMotionTracker.getNewPosition(oldLine, 1).map(position -> position.line);

        Assert.assertEquals(expectedNewLine, newLine);
    }

    private void performTest(String oldFile, String newFile, Consumer<DiagnosticPositionTracker> test) throws IOException, DiffException {
        List<String> oldFileSrc = readTestFile(oldFile);
        List<String> newFileSrc = readTestFile(newFile);

//        Context oldFileContext = getContextForFile(oldFile);
//        Context newFileContext = getContextForFile(newFile);

        DiagnosticPositionTracker positionTracker = DPTrackerConstructorFactory
                .newTokenizedLineTracker().create(new SrcFile(oldFile, oldFileSrc), new SrcFile(newFile, newFileSrc));

        test.accept(positionTracker);
    }

    @Test
    public void canTrackSingleLineMove() throws IOException, DiffException {
        performTest("foo_1.java", "foo_2.java", lineMotionTracker -> {
            assertLineNumberIsTracked(lineMotionTracker, 3, Optional.of(4L));
        });
    }

    @Test
    public void canTrackLargerFileChanges() throws IOException, DiffException {
        performTest("foo_2.java", "foo_4.java", lineMotionTracker -> {
            assertLineNumberIsTracked(lineMotionTracker, 1, Optional.of(1L));
            assertLineNumberIsTracked(lineMotionTracker, 2, Optional.of(6L));
            assertLineNumberIsTracked(lineMotionTracker, 3, Optional.empty());
            assertLineNumberIsTracked(lineMotionTracker, 5, Optional.of(11L));
        });
    }

    @Test
    public void canTrackLinesAroundADeletion() throws IOException, DiffException {
        performTest("Tag.java", "Tag_Newer.java", lineMotionTracker -> {
            assertLineNumberIsTracked(lineMotionTracker, 18, Optional.of(20L));
            assertLineNumberIsTracked(lineMotionTracker, 19, Optional.empty());
            assertLineNumberIsTracked(lineMotionTracker, 20, Optional.empty());
            assertLineNumberIsTracked(lineMotionTracker, 21, Optional.of(21L));
        });
    }

    @Test
    public void canTrackLinesAroundAnInsertion() throws IOException, DiffException {
        performTest("Tag.java", "Tag_Newer.java", lineMotionTracker -> {
            assertLineNumberIsTracked(lineMotionTracker, 18, Optional.of(20L));
            assertLineNumberIsTracked(lineMotionTracker, 19, Optional.empty());
            assertLineNumberIsTracked(lineMotionTracker, 20, Optional.empty());
            assertLineNumberIsTracked(lineMotionTracker, 21, Optional.of(21L));
        });
    }

    @Test
    public void isInsensitiveToWhitespace() throws IOException, DiffException {
        performTest("foo_2.java", "foo_4_indented.java", lineMotionTracker -> {
            assertLineNumberIsTracked(lineMotionTracker, 1, Optional.of(1L));
            assertLineNumberIsTracked(lineMotionTracker, 2, Optional.of(6L));
            assertLineNumberIsTracked(lineMotionTracker, 3, Optional.empty());
            assertLineNumberIsTracked(lineMotionTracker, 5, Optional.of(11L));
            assertLineNumberIsTracked(lineMotionTracker, 4, Optional.of(7L));
        });
    }

    @Test
    public void canTrackIndentation() throws IOException, DiffException {
        performTest("foo_2.java", "foo_4_indented.java", lineMotionTracker -> {
            // GIVEN:
            DiagnosticPosition oldPos = new DiagnosticPosition(1, 1);

            // WHEN:
            Optional<DiagnosticPosition> newPos = lineMotionTracker.getNewPosition(oldPos);

            // THEN:
            Assert.assertEquals(Optional.of(new DiagnosticPosition(1, 5)), newPos);
        });
    }
}
