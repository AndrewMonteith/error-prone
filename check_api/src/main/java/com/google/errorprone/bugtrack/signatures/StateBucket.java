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

import com.google.common.collect.ImmutableList;
import com.google.errorprone.VisitorState;
import com.sun.source.tree.Tree;
import com.sun.source.util.TreePath;

import java.util.ArrayList;
import java.util.List;

/*
    Deep copies information from the VisitorState we want to use later to consturct signatures.
 */
public final class StateBucket {
    public ImmutableList<Tree.Kind> path;

    private ImmutableList<Tree.Kind> copyPath(TreePath path) {
        List<Tree.Kind> pathFromNodeToRoot = new ArrayList<>();

        while (path != null) {
            pathFromNodeToRoot.add(path.getLeaf().getKind());
            path = path.getParentPath();
        }

        return ImmutableList.copyOf(pathFromNodeToRoot);
    }

    public StateBucket(VisitorState state) {
        this.path = copyPath(state.getPath());
    }

}
