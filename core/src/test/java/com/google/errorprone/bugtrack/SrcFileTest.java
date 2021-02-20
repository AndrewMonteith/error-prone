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

import com.google.errorprone.bugtrack.motion.SrcFile;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

import static com.google.errorprone.bugtrack.TestUtils.readTestFile;

public class SrcFileTest {

    @Test
    public void correctlyKnownsLinesAndColumns() throws IOException {
        // GIVEN:
        List<String> oldSrc = readTestFile("foo_1.java");

        // WHEN:
        SrcFile s = new SrcFile("foo_1.java", oldSrc);

        // THEN:
        Assert.assertEquals(1, s.getLineNumber(0));
        Assert.assertEquals(1, s.getColumnNumber(0));

        Assert.assertEquals(2, s.getLineNumber(12));
        Assert.assertEquals(1, s.getColumnNumber(12));

        Assert.assertEquals(3, s.getLineNumber(37));
        Assert.assertEquals(1, s.getColumnNumber(37));
    }

    @Test
    public void correctExtractsFragments() throws IOException {
        // GIVEN:
        List<String> oldSrc = readTestFile("foo_1.java");

        // WHEN:
        SrcFile s = new SrcFile("foo_1.java", oldSrc);

        // THEN:
        Assert.assertEquals("class Foo", s.getSrcExtract(1, 9));
        Assert.assertEquals("public void main() {", s.getSrcExtract(17, 36));
    }

}
