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
import org.eclipse.jdt.core.dom.ASTNode;

public class JDTToJCPosMapper {
  private final EndPosTable endPosTable;
  private final JCTree.JCCompilationUnit ast;

  public JDTToJCPosMapper(JCTree.JCCompilationUnit ast) {
    this.endPosTable = ast.endPositions;
    this.ast = ast;
  }

  public NodeLocation map(ITree node) {
    if (isDocNode(node)) {
      // Doc comment's positions are all the same in diagnostics
      return new NodeLocation(node.getPos(), node.getPos(), node.getPos());
    } else {
      return mapNonDocNode(node);
    }
  }

  private boolean isDocNode(ITree node) {
    while (node != null) {
      if (node.getType() == ASTNode.JAVADOC) {
        return true;
      }
      node = node.getParent();
    }
    return false;
  }

  private NodeLocation mapNonDocNode(ITree node) {
    NonDocPosMapper startPosMapper = new NonDocPosMapper(endPosTable, node.getPos());
    startPosMapper.scan(ast, null);

    NonDocPosMapper endPosMapper = new NonDocPosMapper(endPosTable, node.getPos());
    endPosMapper.scan(ast, null);

    return new NodeLocation(
        startPosMapper.closestStartPosition,
        startPosMapper.closestPreferredPosition,
        endPosMapper.closestEndPosition);
  }

  private static class NonDocPosMapper extends TreeScanner<Void, Void> {
    private final EndPosTable endPosTable;
    private final long jdtPos;

    private long closestStartPosition = -1;
    private long closestEndPosition = Long.MAX_VALUE;
    private long closestPreferredPosition = -1;

    public NonDocPosMapper(EndPosTable endPosTable, final long jdtPos) {
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

            // Update preferred position if it's closer
            final int prefPos = jcTree.getPreferredPosition();
            if (Math.abs(prefPos - jdtPos) < Math.abs(prefPos - closestPreferredPosition)) {
              closestPreferredPosition = prefPos;
            }
          }
          tree.accept(this, p);
        }
      }

      return null;
    }
  }
}
