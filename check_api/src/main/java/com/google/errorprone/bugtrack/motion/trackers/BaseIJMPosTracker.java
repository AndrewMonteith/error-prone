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
import com.google.errorprone.bugtrack.DatasetDiagnostic;
import com.google.errorprone.bugtrack.SrcPairInfo;
import com.google.errorprone.bugtrack.motion.SrcFile;
import com.google.errorprone.bugtrack.utils.ThrowingFunction;
import com.sun.tools.javac.tree.JCTree;
import org.eclipse.jdt.core.dom.ASTNode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public abstract class BaseIJMPosTracker {
  protected final TreeContext oldSrcTree;
  protected final TreeContext newSrcTree;
  protected final MappingStore mappings;
  private final SrcPairInfo srcPairInfo;

  protected BaseIJMPosTracker(
      SrcPairInfo srcPairInfo, ThrowingFunction<SrcFile, AbstractJdtVisitor> jdtVisitorFunc)
      throws IOException {
    this.srcPairInfo = srcPairInfo;

    this.oldSrcTree = parseFileWithVisitor(srcPairInfo.files.oldFile, jdtVisitorFunc);
    this.newSrcTree = parseFileWithVisitor(srcPairInfo.files.newFile, jdtVisitorFunc);
    this.mappings = new MappingStore();

    new JavaMatchers.IterativeJavaMatcher_V2(oldSrcTree.getRoot(), newSrcTree.getRoot(), mappings)
        .match();
  }

  public BaseIJMPosTracker(SrcPairInfo srcPairInfo) throws IOException {
    this(srcPairInfo, BetterJdtVisitor::new);
  }

  private static TreeContext parseFileWithVisitor(
      SrcFile file, ThrowingFunction<SrcFile, AbstractJdtVisitor> jdtVisitorFunc)
      throws IOException {
    String src = file.getSrc();

    AbstractJdtTreeGenerator treeGenerator =
        new AbstractJdtTreeGenerator() {
          @Override
          protected AbstractJdtVisitor createVisitor() {
            return jdtVisitorFunc.apply(file);
          }
        };

    return treeGenerator.generateFromString(src);
  }

  private Optional<ITree> findFirstMatchingJDTNode(final long pos) {
    for (ITree node : oldSrcTree.getRoot().postOrder()) {
      if (!node.isMatched()) {
        continue;
      }

      if (ITreeUtils.encompasses(node, pos)) {
        return Optional.of(node);
      }
    }

    return Optional.empty();
  }

  private ITree findClosestMatchingNodeInJavadoc(ITree node, final long pos) {
    // Necessary since JDT's structure for Javadoc's is kinda weird in that there may be
    // nodes with overlapping [pos, pos+length] but not overlapping labels. For example without
    // this we could not track
    //   guice dafa4b0bec4e7ec5e1df75e3fb9a2fdf4920921a .. dafa4b0bec4e7ec5e1df75e3fb9a2fdf4920921a
    //   core/src/com/google/inject/internal/InternalInjectorCreator.java 83 52 3198 3198 3198
    //   [InvalidLink] The reference `#requireExplicitBindings()` to a method doesn't resolve to
    // 3198 refers to 'Returns true if' and '@link' nodes in the tree, without this code the
    // 'Returns true if' node is matched first in postOrder traversal and so mapped to the wrong
    // position.
    while (node.getType() != ASTNode.JAVADOC) {
      node = node.getParent();
    }

    ITree closest = null;
    for (ITree desc : node.postOrder()) {
      if (!ITreeUtils.encompasses(desc, pos)) {
        continue;
      }

      if (closest == null || ITreeUtils.isCloser(pos, desc.getPos(), closest.getPos())) {
        closest = desc;
      }
    }

    return closest;
  }

  private Optional<ITree> findClosestMatchingJDTNode(final long pos) {
    return findFirstMatchingJDTNode(pos)
        .map(
            matchingNode -> {
              if (ITreeUtils.inDocNode(matchingNode)) {
                matchingNode = findClosestMatchingNodeInJavadoc(matchingNode, pos);
              }

              return matchingNode;
            });
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
              + srcPairInfo.files.oldFile.getName()
              + "\n"
              + "New file "
              + srcPairInfo.files.newFile.getName()
              + "\n";

      throw new RuntimeException(msg);
    }

    return node;
  }

  private NodeLocation mapJdtSrcRangeToJCSrcRange(ITree jdtNode) {
    // Find the src buffer range of the closest JC node to matched jdt node's start position
    JCTree.JCCompilationUnit newJCAst = srcPairInfo.loadNewJavacAST();
    return new JDTToJCMapper(newJCAst).map(jdtNode);
  }

  protected Optional<NodeLocation> trackStartNodePosition(final long startPos) {
    return findClosestMatchingJDTNode(startPos)
        .map(closestOldJdtNode -> mapJdtSrcRangeToJCSrcRange(mappings.getDst(closestOldJdtNode)));
  }

  private long modifyEndPosition(DatasetDiagnostic diagnostic) {
    switch (diagnostic.getType()) {
      case "MultiVariableDeclaration":
      case "FieldCanBeFinal":
      case "InitializeInline":
      case "MemberName":
      case "UnusedVariable":
        return diagnostic.getEndPos() - 1;
      default:
        return diagnostic.getEndPos();
    }
  }

  protected Optional<List<NodeLocation>> trackEndPosition(DatasetDiagnostic diagnostic) {
    // We return a list since we're going to consider multiple cases for a possible end position
    // If the position refers to a non-generic class (determined syntatically) then we merely return
    // one location by tracking the token. Else we track both the token and the 'token<...>'
    final long endPos = modifyEndPosition(diagnostic);

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
}
