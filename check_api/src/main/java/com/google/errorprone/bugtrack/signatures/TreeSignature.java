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

import com.google.errorprone.VisitorState;
import com.sun.source.tree.Tree;
import com.sun.source.util.TreePath;

import java.util.ArrayList;
import java.util.List;

public class TreeSignature implements DiagnosticSignature {
    private List<Tree.Kind> getKindsFromNodeToRoot(TreePath path) {
        List<Tree.Kind> kindPath = new ArrayList<>();

        while (path != null) {
            kindPath.add(path.getLeaf().getKind());
            System.out.println(path.getLeaf().getKind() + " " + path.getLeaf());
            path = path.getParentPath();
        }

        return kindPath;
    }

    public TreeSignature(VisitorState state) {
        List<Tree.Kind> kinds = getKindsFromNodeToRoot(state.getPath());

        kinds.forEach(System.out::println);
    }

    @Override
    public boolean areSame(DiagnosticSignature other) {
        return true;
    }
}
