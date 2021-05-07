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

import com.github.gumtreediff.gen.jdt.AbstractJdtTreeGenerator;
import com.github.gumtreediff.gen.jdt.AbstractJdtVisitor;
import com.github.gumtreediff.tree.ITree;
import com.google.common.collect.ImmutableList;
import com.google.errorprone.bugtrack.motion.SrcFile;
import com.google.errorprone.bugtrack.motion.trackers.BetterJdtVisitor;
import com.google.googlejavaformat.java.FormatterException;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

import static com.google.errorprone.bugtrack.motion.trackers.ITreeUtils.countNumberOfVariableIdentifiers;

public class ITreeUtilsTest {

  private static ITree parseSource(String... source) throws IOException, FormatterException {
    ImmutableList<String> lines = ImmutableList.copyOf(source);
    SrcFile f = new SrcFile("Foo.java", lines);

    AbstractJdtTreeGenerator treeGenerator =
        new AbstractJdtTreeGenerator() {
          @Override
          protected AbstractJdtVisitor createVisitor() {
            try {
              return new BetterJdtVisitor(new SrcFile("foo.java", lines));
            } catch (FormatterException e) {
              throw new RuntimeException(e);
            }
          }
        };

    return treeGenerator.generateFromString(f.getSrc()).getRoot();
  }

  @Test
  public void countSingleVariableDeclarations() throws FormatterException, IOException {
    ITree root =
        parseSource(
            "public class Foo {",
            "  private int x;",
            "  private iny y = x;",
            "  private List<String> foobar;",
            "  public void foo(int z) {",
            "    int u;",
            "    int v = 1;",
            "  }");

    Assert.assertEquals(1, countNumberOfVariableIdentifiers(root, 21));
    Assert.assertEquals(1, countNumberOfVariableIdentifiers(root, 29));
    Assert.assertEquals(1, countNumberOfVariableIdentifiers(root, 38));
    Assert.assertEquals(1, countNumberOfVariableIdentifiers(root, 45));
    Assert.assertEquals(1, countNumberOfVariableIdentifiers(root, 59));
    Assert.assertEquals(1, countNumberOfVariableIdentifiers(root, 65));
    Assert.assertEquals(1, countNumberOfVariableIdentifiers(root, 106));
    Assert.assertEquals(1, countNumberOfVariableIdentifiers(root, 119));
    Assert.assertEquals(1, countNumberOfVariableIdentifiers(root, 130));
  }

  @Test
  public void countMultipleVariableDeclarations() throws FormatterException, IOException {
    ITree root =
        parseSource(
            "public class Foo {",
            "  private int x, y, z;",
            "  private Map<String, Integer> u, v",
            "  public void foo(int x, int y) {",
            "    int u, v = 1;",
            "    List<String> x, y, z;",
            "  }",
            "}");

    Assert.assertEquals(3, countNumberOfVariableIdentifiers(root, 28));
    Assert.assertEquals(2, countNumberOfVariableIdentifiers(root, 50));
    Assert.assertEquals(1, countNumberOfVariableIdentifiers(root, 96));
    Assert.assertEquals(1, countNumberOfVariableIdentifiers(root, 103));
    Assert.assertEquals(2, countNumberOfVariableIdentifiers(root, 116));
    Assert.assertEquals(3, countNumberOfVariableIdentifiers(root, 134));
  }
}
