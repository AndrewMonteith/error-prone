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
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Streams;
import com.google.errorprone.ErrorProneFlags;
import org.eclipse.jdt.core.dom.ASTNode;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

public final class ITreeUtils {
  private ITreeUtils() {}

  public static boolean inDocNode(ITree node) {
    while (node != null) {
      if (node.getType() == ASTNode.JAVADOC) {
        return true;
      }
      node = node.getParent();
    }
    return false;
  }

  public static boolean encompasses(ITree node, final long pos) {
    return node.getPos() <= pos && pos <= (node.getPos() + node.getLength());
  }

  public static boolean isCloser(final long truePos, final long pos1, final long pos2) {
    return Math.abs(truePos - pos1) < Math.abs(truePos - pos2);
  }

  public static ITree findClosestMatchingNodeInJavadoc(ITree node, final long pos) {
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

    ITree closest = node;
    for (ITree desc : node.postOrder()) {
      if (!(ITreeUtils.encompasses(desc, pos) && desc.isMatched())) {
        continue;
      }

      if (ITreeUtils.isCloser(pos, desc.getPos(), closest.getPos())) {
        closest = desc;
      }
    }

    return closest;
  }

  private static ITree processIfDocNode(ITree node, final long pos) {
    return inDocNode(node) ? findClosestMatchingNodeInJavadoc(node, pos) : node;
  }

  private static Optional<ITree> findHighestMatchedNodeByPos(
      ITree root, Function<ITree, Integer> nodePos, final int pos) {
    ITree current = root;

    next_node:
    while (nodePos.apply(current) != pos || !current.isMatched()) {
      List<ITree> children = current.getChildren();

      for (int i = 0; i < children.size(); ++i) {
        ITree child = children.get(i);
        if (encompasses(child, pos)) {
          if (i < children.size() - 1 && nodePos.apply(children.get(i+1)) == pos) {
            continue;
          }

          current = child;
          continue next_node;
        }
      }

      return Optional.empty();
    }

    return Optional.of(current);
  }

  public static Optional<ITree> findHighestMatchedNodeWithPos(ITree root, final int pos) {
    return findHighestMatchedNodeByPos(root, ITree::getPos, pos);
  }

  public static Optional<ITree> findHighestMatchedNodeWithEndPos(ITree root, final int pos) {
    return findHighestMatchedNodeByPos(root, ITree::getEndPos, pos);
  }

  public static Optional<ITree> findHighestNodeWithPos(ITree root, final long pos) {
    ITree current = root;

    next_node:
    while (current.getPos() != pos) {
      List<ITree> children = current.getChildren();

      for (int i = 0; i < children.size(); ++i) {
        ITree child = children.get(i);
        if (encompasses(child, pos)) {
          if (i < children.size() - 1 && children.get(i+1).getPos() == pos) {
            continue;
          }

          current = child;
          continue next_node;
        }
      }

      return Optional.empty();
    }

    return Optional.of(current);
  }

  public static Optional<ITree> findLowestNodeEncompassing(ITree root, final int pos) {
    ITree current = root;
    next_node:
    while (true) {
      for (ITree child : current.getChildren()) {
        if (encompasses(child, pos)) {
          current = child;
          continue next_node;
        }
      }

      return Optional.of(processIfDocNode(current, pos));
    }
  }

  public static ITree getVarDeclNode(ITree root, final int pos) {
    ImmutableSet<Integer> multiVarIds =
        ImmutableSet.of(
            ASTNode.VARIABLE_DECLARATION_EXPRESSION,
            ASTNode.VARIABLE_DECLARATION_STATEMENT,
            ASTNode.FIELD_DECLARATION,
            ASTNode.SINGLE_VARIABLE_DECLARATION);

    ITree node = findLowestNodeEncompassing(root, pos).get();

    while (!multiVarIds.contains(node.getType())) {
      node = node.getParent();
    }

    return node;
  }

  public static int countNumberOfVariableIdentifiers(ITree root, final int startPos) {
    ITree varDeclNodes = getVarDeclNode(root, startPos);

    if (varDeclNodes.getType() == ASTNode.SINGLE_VARIABLE_DECLARATION) {
      return 1;
    }

    return Streams.stream(varDeclNodes.preOrder())
        .mapToInt(node -> node.getType() == ASTNode.VARIABLE_DECLARATION_FRAGMENT ? 1 : 0)
        .sum();
  }
}
