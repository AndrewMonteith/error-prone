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

package com.google.errorprone.bugtrack.motion.trackers;

import com.github.gumtreediff.tree.ITree;
import com.google.common.collect.ImmutableMap;
import com.sun.source.tree.Tree;
import com.sun.source.util.TreeScanner;
import com.sun.tools.javac.tree.EndPosTable;
import com.sun.tools.javac.tree.JCTree;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class PreferredPositionMap {
  private final ImmutableMap<NodeRegion, Integer> preferredPositions;
  private final EndPosTable endPosTable;

  public PreferredPositionMap(JCTree.JCCompilationUnit jcCompilationUnit) {
    this.endPosTable = jcCompilationUnit.endPositions;

    PreferredPosMapper mapper = new PreferredPosMapper(endPosTable);
    mapper.scan(jcCompilationUnit, null);
    preferredPositions = ImmutableMap.copyOf(mapper.prefPosMap);
  }

  public int getPreferredPosition(ITree node) {
    return preferredPositions.getOrDefault(NodeRegion.of(node), node.getPos());
  }

  private static class PreferredPosMapper extends TreeScanner<Void, Void> {
    private final EndPosTable endPosTable;
    private final Map<NodeRegion, Integer> prefPosMap;

    PreferredPosMapper(EndPosTable endPosTable) {
      this.endPosTable = endPosTable;
      this.prefPosMap = new HashMap<>();
    }

    @Override
    public Void scan(Tree tree, Void p) {
      if (tree instanceof JCTree) {
        JCTree jcTree = (JCTree) tree;

        if (jcTree.getPreferredPosition() != jcTree.getStartPosition()) {
          prefPosMap.put(NodeRegion.of(jcTree, endPosTable), jcTree.getPreferredPosition());
        }
      }

      return super.scan(tree, p);
    }
  }

  private static class NodeRegion {
    final int startPos;
    final int endPos;

    NodeRegion(final int startPos, final int endPos) {
      this.startPos = startPos;
      this.endPos = endPos;
    }

    static NodeRegion of(JCTree tree, EndPosTable endPosTable) {
      return new NodeRegion(tree.getStartPosition(), tree.getEndPosition(endPosTable));
    }

    static NodeRegion of(ITree node) {
      return new NodeRegion(node.getPos(), node.getEndPos());
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      NodeRegion that = (NodeRegion) o;
      return startPos == that.startPos && endPos == that.endPos;
    }

    @Override
    public int hashCode() {
      return Objects.hash(startPos, endPos);
    }
  }
}
