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
import org.eclipse.jdt.core.dom.ASTNode;

import java.util.ArrayDeque;
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
    if (ITreeUtils.inDocNode(node)) {
      node = findClosestMatchingNodeInJavadoc(node, pos);
    }

    return node;
  }

  private static <T> Optional<T> findFirst(Iterable<T> iter, Predicate<T> pred) {
    for (T t : iter) {
      if (pred.test(t)) {
        return Optional.of(t);
      }
    }

    return Optional.empty();
  }

  /* Lower = Furthest from root */
  public static Optional<ITree> findLowestNodeThat(ITree root, Predicate<ITree> acceptNode) {
    return findFirst(root.postOrder(), acceptNode);
  }

  public static Optional<ITree> findLowestMatchedNodeThat(ITree root, Predicate<ITree> acceptNode) {
    return findFirst(root.postOrder(), acceptNode.and(ITree::isMatched));
  }

  /* Highest = Closest to root */
  public static Optional<ITree> findHighestNodeThat(ITree root, Predicate<ITree> acceptNode) {
    return findFirst(root.preOrder(), acceptNode);
  }

  private static Optional<ITree> findHighestMatchedNodeByPos(
      ITree root, Function<ITree, Integer> nodePos, final int pos) {
    ITree current = root;
    next_node:
    while (nodePos.apply(current) != pos && !current.isMatched()) {
      if (current.getChildren().isEmpty()) {
        return Optional.empty();
      }

      for (ITree child : current.getChildren()) {
        if (encompasses(child, pos)) {
          current = child;
          continue next_node;
        }
      }
    }

    return Optional.of(current);
  }

  public static Optional<ITree> findHighestMatchedNodeWithPos(ITree root, final int pos) {
    return findHighestMatchedNodeByPos(root, ITree::getPos, pos);
  }

  public static Optional<ITree> findHighestMatchedNodeWithEndPos(ITree root, final int pos) {
    return findHighestMatchedNodeByPos(root, ITree::getEndPos, pos);
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

      return Optional.of(current);
    }
  }

  public static Optional<ITree> findLowestMatchedNodeWithPos(ITree root, final int pos) {
    ITree current = root;
    ITree lowest = null;

    next_node:
    while (true) {
      if (current.getPos() == pos && current.isMatched()) {
        lowest = current;
      }

      for (ITree child : current.getChildren()) {
        if (encompasses(child, pos)) {
          current = child;
          continue next_node;
        }
      }

      break;
    }

    return lowest != null ? Optional.of(lowest) : Optional.empty();
  }

  public static Optional<ITree> findHighestMatchedNodeThat(
      ITree root, Predicate<ITree> acceptNode) {
    return findFirst(root.preOrder(), acceptNode.and(ITree::isMatched));
  }

  //  public static Optional<ITree> findDeepestNodeThat(ITree root, Predicate<ITree> acceptNode) {
  //    for (ITree node : root.postOrder()) {
  //      if (acceptNode.test(node)) {
  //        if (node.isMatched()) {
  //          return Optional.of(node);
  //        } else {
  //          return Optional.empty();
  //        }
  //      }
  //    }
  //
  //    return Optional.empty();
  //  }
  //
  //  public static Optional<ITree> findDeepestMatchingNodeThat(
  //      ITree root, Predicate<ITree> acceptNode) {
  //    for (ITree node : root.postOrder()) {
  //      if (node.isMatched() && acceptNode.test(node)) {
  //        return Optional.of(node);
  //      }
  //    }
  //
  //    return Optional.empty();
  //  }
  //
  //  public static Optional<ITree> findShallowestNodeThat(ITree root, Predicate<ITree> acceptNode)
  // {
  //    for (ITree node : root.preOrder()) {
  //      if (node.isMatched() && acceptNode.test(node)) {
  //        return Optional.of(node);
  //      }
  //    }
  //
  //    return Optional.empty();
  //  }
  //
  //  public static Optional<ITree> findDeepestNodeEncompassing(ITree root, final long pos) {
  //    return findDeepestNodeThat()
  //  }
  //
  //  public static Optional<ITree> findDeepestMatchingNodeEncompassing(ITree root, final long pos)
  // {
  //    return findDeepestNodeThat(root, node -> encompasses(node, pos))
  //        .map(
  //            matchingNode -> {
  //              if (inDocNode(matchingNode)) {
  //                matchingNode = findClosestMatchingNodeInJavadoc(matchingNode, pos);
  //              }
  //
  //              return matchingNode;
  //            });
  //  }

  //  public static Optional<ITree> findClosestNode(ITree root, final long pos) {
  //    return findFirstMatchingJDTNodeThat(root, node -> encompasses(node, pos))
  //        .map(matchingNode -> furtherProcessIfDocNode(matchingNode, pos));
  //  }
  //
  //  public static Optional<ITree> findClosestMatchedNodeThat(ITree root, Predicate<ITree> test) {
  //    return findFirstMatchingJDTNodeThat(root, test)
  //        .map(matchingNode -> furtherProcessIfDocNode(matchingNode, matchingNode.getPos()));
  //  }
}
