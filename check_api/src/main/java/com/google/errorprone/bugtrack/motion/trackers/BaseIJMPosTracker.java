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

import at.aau.softwaredynamics.matchers.JavaMatchers;
import com.github.gumtreediff.gen.jdt.AbstractJdtTreeGenerator;
import com.github.gumtreediff.gen.jdt.AbstractJdtVisitor;
import com.github.gumtreediff.matchers.MappingStore;
import com.github.gumtreediff.tree.ITree;
import com.github.gumtreediff.tree.TreeContext;
import com.google.errorprone.bugtrack.motion.SrcFile;
import com.google.errorprone.bugtrack.motion.SrcFilePair;
import com.google.errorprone.bugtrack.utils.IOThrowingFunction;
import com.sun.tools.javac.tree.JCTree;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public abstract class BaseIJMPosTracker {
  private final TrackersSharedState sharedState;
  private final SrcFilePair srcFilePair;

  private final TreeContext oldSrcTree;
  private final TreeContext newSrcTree;
  private final MappingStore mappings;

  protected BaseIJMPosTracker(
      SrcFilePair srcFilePair,
      TrackersSharedState sharedState,
      IOThrowingFunction<SrcFile, AbstractJdtVisitor> jdtVisitorSupplier)
      throws IOException {
    this.sharedState = sharedState;
    this.srcFilePair = srcFilePair;
    this.oldSrcTree =
        parseFileWithVisitor(srcFilePair.oldFile, jdtVisitorSupplier.apply(srcFilePair.oldFile));
    this.newSrcTree =
        parseFileWithVisitor(srcFilePair.newFile, jdtVisitorSupplier.apply(srcFilePair.newFile));
    this.mappings = new MappingStore();

    new JavaMatchers.IterativeJavaMatcher_V2(oldSrcTree.getRoot(), newSrcTree.getRoot(), mappings)
        .match();
  }

  public BaseIJMPosTracker(SrcFilePair srcFilePair, TrackersSharedState sharedState)
      throws IOException {
    this(srcFilePair, sharedState, BetterJdtVisitor::new);
  }

  private static TreeContext parseFileWithVisitor(SrcFile file, AbstractJdtVisitor visitor)
      throws IOException {
    String src = file.getSrc();

    AbstractJdtTreeGenerator treeGenerator =
        new AbstractJdtTreeGenerator() {
          @Override
          protected AbstractJdtVisitor createVisitor() {
            return visitor;
          }
        };

    return treeGenerator.generateFromString(src);
  }

  private Optional<ITree> findClosestMatchingJDTNode(final long pos) {
    for (ITree node : oldSrcTree.getRoot().postOrder()) {
      if (!node.isMatched()) {
        continue;
      }

      if (node.getPos() <= pos && pos <= node.getPos() + node.getLength()) {
        return Optional.of(node);
      }
    }

    return Optional.empty();
  }

  private boolean isTemplatedClassNode(ITree tree) {
    if (tree.getLabel().isEmpty()) {
      return false;
    }

    String className = tree.getLabel();
    String templatedClassName = className + "<";
    while (tree != null && tree.getLabel().startsWith(className)) {
      if (tree.getLabel().startsWith(templatedClassName)) {
        return true;
      }

      tree = tree.getParent();
    }

    return false;
  }

  private ITree findNodeWithAngleBrackets(ITree tree) {
    ITree node = tree;
    while (node != null && !node.getLabel().endsWith(">")) {
      node = node.getParent();
    }

    if (node == null) {
      String msg =
          "class began with < but couldn't find >\n"
              + "Tree label "
              + tree.getLabel()
              + "\n"
              + "Old file "
              + srcFilePair.oldFile.getName()
              + "\n"
              + "New file "
              + srcFilePair.newFile.getName()
              + "\n";

      throw new RuntimeException(msg);
    }

    return node;
  }

  private NodeLocation mapJdtSrcRangeToJCSrcRange(ITree jdtNode) {
    // Find the src buffer range of the closest JC node to matched jdt node's start position
    JCTree.JCCompilationUnit newJCAst = sharedState.loadNewJavacAST(srcFilePair);
    JDTToJCPosMapper startPosMapper = new JDTToJCPosMapper(newJCAst.endPositions, jdtNode.getPos());
    startPosMapper.scan(newJCAst, null);

    JDTToJCPosMapper endPosMapper =
        new JDTToJCPosMapper(newJCAst.endPositions, jdtNode.getEndPos());
    endPosMapper.scan(newJCAst, null);

    return new NodeLocation(
        startPosMapper.getClosestStartPosition(),
        startPosMapper.getClosestPreferredPosition(),
        endPosMapper.getClosestEndPosition());
  }

  protected Optional<NodeLocation> trackPosition(final long startPos) {
    return findClosestMatchingJDTNode(startPos)
        .map(closestOldJdtNode -> mapJdtSrcRangeToJCSrcRange(mappings.getDst(closestOldJdtNode)));
  }

  protected Optional<List<NodeLocation>> trackEndPosition(final long endPos) {
    // We return a list since we're going to consider multiple cases for a possible end position
    // If the position refers to a non-generic class (determined syntatically) then we merely return
    // one location
    // by tracking the token. Else we track both the token and the 'token<...>'
    return findClosestMatchingJDTNode(endPos)
        .map(
            closestOldJdtNode -> {
              ITree newJdtNode = mappings.getDst(closestOldJdtNode);

              List<NodeLocation> locations = new ArrayList<>();
              if (isTemplatedClassNode(newJdtNode)) {
                locations.add(mapJdtSrcRangeToJCSrcRange(findNodeWithAngleBrackets(newJdtNode)));
              }

              locations.add(mapJdtSrcRangeToJCSrcRange(newJdtNode));

              return locations;
            });
  }

  protected static class NodeLocation {
    final long start;
    final long end;
    final long pos;

    private NodeLocation(final long start, final long pos, final long end) {
      this.start = start;
      this.pos = pos;
      this.end = end;
    }
  }
}
