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
import com.sun.source.util.TreePath;

public class TreeSignature implements DiagnosticSignature {
    private final TreePath path;
        // Not sure if keeping a reference to this will keep some very expensive AST data structures alive.
        // But I guess we'll find out.  If so we can just store a hash instead of the entire path.

    public TreeSignature(VisitorState state) {
        this.path = state.getPath();
    }

    @Override
    public boolean areSame(DiagnosticSignature other) {
        return true;
    }
}
