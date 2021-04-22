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
import com.sun.source.tree.Tree;
import com.sun.source.util.TreeScanner;
import com.sun.tools.javac.tree.EndPosTable;
import com.sun.tools.javac.tree.JCTree;

public class JDTToJCMapper {
  private final EndPosTable endPosTable;
  private final JCTree.JCCompilationUnit ast;

  public JDTToJCMapper(JCTree.JCCompilationUnit ast) {
    this.endPosTable = ast.endPositions;
    this.ast = ast;
  }

  public NodeLocation map(ITree node) {
    if (ITreeUtils.inDocNode(node)) {
      // Doc comment's positions are all the same in diagnostics
      return NodeLocation.single(node.getPos());
    } else {
      return mapNonDocNode(node);
    }
  }

  public NodeLocation map(final long pos) {
    PreferredPosMapper prefPosMapper = new PreferredPosMapper(pos);
    prefPosMapper.scan(ast, null);
    return NodeLocation.single(prefPosMapper.closestPreferredPosition);
  }

  private NodeLocation mapNonDocNode(ITree node) {
    NonDocNodeMapper startPosMapper = new NonDocNodeMapper(endPosTable, node.getPos());
    startPosMapper.scan(ast, null);

    NonDocNodeMapper endPosMapper = new NonDocNodeMapper(endPosTable, node.getEndPos());
    endPosMapper.scan(ast, null);

    return new NodeLocation(startPosMapper.closestStartPosition, endPosMapper.closestEndPosition);
  }

  private static class PreferredPosMapper extends TreeScanner<Void, Void> {
    private final long jdtPos;

    private long closestPreferredPosition = Long.MIN_VALUE;

    public PreferredPosMapper(final long jdtPos) {
      this.jdtPos = jdtPos;
    }

    @Override
    public Void scan(Tree tree, Void p) {
      if (tree instanceof JCTree) {
        final int prefPos = ((JCTree) tree).getPreferredPosition();

        if (ITreeUtils.isCloser(jdtPos, prefPos, closestPreferredPosition)) {
          closestPreferredPosition = prefPos;
        }
      }

      return null;
    }
  }

  private static class NonDocNodeMapper extends TreeScanner<Void, Void> {
    private final EndPosTable endPosTable;
    private final long jdtPos;

    private long closestStartPosition = -1;
    private long closestEndPosition = Long.MAX_VALUE;

    public NonDocNodeMapper(EndPosTable endPosTable, final long jdtPos) {
      this.endPosTable = endPosTable;
      this.jdtPos = jdtPos;
    }

    @Override
    public Void scan(Tree tree, Void p) {
      if (tree instanceof JCTree) {
        JCTree jcTree = (JCTree) tree;

        final long jcStartPos = jcTree.getStartPosition();
        final long jcEndPos = jcTree.getEndPosition(endPosTable);

        if ((jcStartPos < jdtPos && jdtPos <= jcEndPos)
            || (jcStartPos <= jdtPos && jdtPos < jcEndPos)) {
          // If node is strictly closer then accept it
          if (closestStartPosition <= jcStartPos && jcEndPos <= closestEndPosition) {
            closestStartPosition = jcStartPos;
            closestEndPosition = jcEndPos;
          }
          tree.accept(this, p);
        }
      }

      return null;
    }
  }
}
