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
import com.google.common.collect.Iterables;
import com.google.errorprone.ErrorProneInMemoryFileManager;
import com.google.errorprone.bugtrack.motion.LineMotionTracker;
import com.google.errorprone.bugtrack.motion.TokenizedLine;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.tools.javac.api.BasicJavacTask;
import com.sun.tools.javac.api.JavacTaskImpl;
import com.sun.tools.javac.api.JavacTool;
import com.sun.tools.javac.util.Context;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
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
public class TokenLineMotionTackerTest {
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

    private void performTest(String oldFile, String newFile, Consumer<LineMotionTracker<TokenizedLine>> test) throws IOException, DiffException {
        List<String> oldFileSrc = readTestFile(oldFile);
        List<String> newFileSrc = readTestFile(newFile);

        Context oldFileContext = getContextForFile(oldFile);
        Context newFileContext = getContextForFile(newFile);

        LineMotionTracker<TokenizedLine> lineMotionTracker = LineMotionTracker.newLineTokenTracker(
                oldFileSrc, oldFileContext, newFileSrc, newFileContext);

        test.accept(lineMotionTracker);
    }

    @Test
    public void canTrackSingleLineMove() throws IOException, DiffException {
        performTest("foo_1.java", "foo_2.java", lineMotionTracker -> {
            Assert.assertEquals(Optional.of(4L), lineMotionTracker.getNewLine(3));
        });
    }

    @Test
    public void canTrackLargerFileChanges() throws IOException, DiffException {
        performTest("foo_2.java", "foo_4.java", lineMotionTracker -> {
            Assert.assertEquals(Optional.of(1L), lineMotionTracker.getNewLine(1));
            Assert.assertEquals(Optional.of(6L), lineMotionTracker.getNewLine(2));
            Assert.assertEquals(Optional.empty(), lineMotionTracker.getNewLine(3));
            Assert.assertEquals(Optional.of(11L), lineMotionTracker.getNewLine(5));
        });
    }

    @Test
    public void canTrackLinesAroundADeletion() throws IOException, DiffException {
        performTest("Tag.java", "Tag_Newer.java", lineMotionTracker -> {
            Assert.assertEquals(Optional.of(20L), lineMotionTracker.getNewLine(18));
            Assert.assertEquals(Optional.empty(), lineMotionTracker.getNewLine(19));
            Assert.assertEquals(Optional.empty(), lineMotionTracker.getNewLine(20));
            Assert.assertEquals(Optional.of(21L), lineMotionTracker.getNewLine(21));
        });
    }

    @Test
    public void canTrackLinesAroundAnInsertion() throws IOException, DiffException {
        performTest("Tag.java", "Tag_Newer.java", lineMotionTracker -> {
            Assert.assertEquals(Optional.of(20L), lineMotionTracker.getNewLine(18));
            Assert.assertEquals(Optional.empty(), lineMotionTracker.getNewLine(19));
            Assert.assertEquals(Optional.empty(), lineMotionTracker.getNewLine(20));
            Assert.assertEquals(Optional.of(21L), lineMotionTracker.getNewLine(21));
        });
    }

    @Test
    public void isInsensitiveToWhitespace() throws IOException, DiffException {
        performTest("foo_2.java", "foo_4_indented.java", lineMotionTracker -> {
            Assert.assertEquals(Optional.of(1L), lineMotionTracker.getNewLine(1));
            Assert.assertEquals(Optional.of(6L), lineMotionTracker.getNewLine(2));
            Assert.assertEquals(Optional.empty(), lineMotionTracker.getNewLine(3));
            Assert.assertEquals(Optional.of(11L), lineMotionTracker.getNewLine(5));
            Assert.assertEquals(Optional.of(7L), lineMotionTracker.getNewLine(4));
        });
    }
}
