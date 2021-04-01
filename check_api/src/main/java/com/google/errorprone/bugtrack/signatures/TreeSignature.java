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

package com.google.errorprone.bugtrack.signatures;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.sun.source.tree.Tree;
import com.sun.source.util.TreePath;

import java.util.ArrayList;
import java.util.List;

public class TreeSignature implements DiagnosticSignature {
  private final ImmutableList<Tree.Kind> pathFromNodeToRoot;

  public TreeSignature(StateBucket state) {
    this.pathFromNodeToRoot = state.path;
  }

  public TreeSignature(Iterable<Integer> treeKindOrdinals) {
    this.pathFromNodeToRoot =
        ImmutableList.copyOf(
            Iterables.transform(treeKindOrdinals, ordinal -> Tree.Kind.values()[ordinal]));
  }

  private List<Tree.Kind> getKindsFromNodeToRoot(TreePath path) {
    List<Tree.Kind> kindPath = new ArrayList<>();

    while (path != null) {
      kindPath.add(path.getLeaf().getKind());
      path = path.getParentPath();
    }

    return kindPath;
  }

  @Override
  public boolean areSame(DiagnosticSignature other) {
    if (!(other instanceof TreeSignature)) {
      return false;
    }

    return Iterables.elementsEqual(pathFromNodeToRoot, ((TreeSignature) other).pathFromNodeToRoot);
  }

  @Override
  public String toString() {
    return "TreeSignature:"
        + Joiner.on(',').join(Iterables.transform(pathFromNodeToRoot, Enum::ordinal));
  }
}
