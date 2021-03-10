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

import com.google.errorprone.bugtrack.utils.JExpand;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.Arrays;
import java.util.List;

@RunWith(JUnit4.class)
public class JExpandTest {

    private void assertExpansionsAreEqual(String expectedSrc, String inputSrc) {
        // GIVEN:
        String[] expectedSrcLines = expectedSrc.split("\n");
        String[] inputSrcLines = inputSrc.split("\n");

        // WHEN:
        List<String> processed = JExpand.expand(Arrays.asList(inputSrcLines));

        // THEN:
        Assert.assertEquals(Arrays.asList(expectedSrcLines), processed);
    }

    @Test
    public void expandsSrcWithTabsIndentation() {
        String oldSrc = "\t\tfoo\n" +
                "\t\tbar\n" +
                "\t\tyar";

        String newSrc = "        foo\n" +
                "        bar\n" +
                "        yar";

        assertExpansionsAreEqual(newSrc, oldSrc);
    }

    @Test
    public void doesNotChangeTextWithOnlySpaces() {
        String oldSrc = "        foo\n" +
                "        bar\n" +
                "        yar";

        String newSrc = "        foo\n" +
                "        bar\n" +
                "        yar";

        assertExpansionsAreEqual(newSrc, oldSrc);
    }

    @Test
    public void removesIndentationSpikesWithTabsAndSpaces() {
        String oldSrc = "    \t// Hi there\n" +
                "    \t  set1.add('3');\n" +
                "\n" +
                "    \t//Comment";

        String newSrc = "        // Hi there\n" +
                "        set1.add('3');\n" +
                "\n" +
                "        //Comment";

        assertExpansionsAreEqual(newSrc, oldSrc);
    }

}
