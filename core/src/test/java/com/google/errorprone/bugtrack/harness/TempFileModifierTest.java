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

package com.google.errorprone.bugtrack.harness;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

@RunWith(JUnit4.class)
public class TempFileModifierTest {
    private static final String TEST_DATA = "src/test/java/com/google/errorprone/bugtrack/testdata/";

    @Test
    public void resetsFileWhenDestroyed() throws Exception {
        Path xmlFile = Paths.get(TEST_DATA + "/test.xml");
        try (TempFileModifier modifier = new TempFileModifier(xmlFile)) {
            Assert.assertEquals(3169, Files.readAllLines(xmlFile).size());
            Files.write(xmlFile, "extra text".getBytes(), StandardOpenOption.APPEND);
            Assert.assertEquals(3170, Files.readAllLines(xmlFile).size());
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail();
        } finally {
            Assert.assertEquals(3169, Files.readAllLines(xmlFile).size());
        }
    }
}
