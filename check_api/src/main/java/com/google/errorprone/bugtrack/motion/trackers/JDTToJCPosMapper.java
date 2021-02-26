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

import com.sun.source.tree.Tree;
import com.sun.source.util.TreeScanner;
import com.sun.tools.javac.tree.EndPosTable;
import com.sun.tools.javac.tree.JCTree;

public class JDTToJCPosMapper extends TreeScanner<Void, Void> {
    private final EndPosTable endPosTable;
    private final long jdtPos;

    private long closestStartPosition = -1;
    public long getClosestStartPosition() {
        return closestStartPosition;
    }

    private long closestEndPosition = -1;
    public long getClosestEndPosition() {
        return closestEndPosition;
    }

    public JDTToJCPosMapper(EndPosTable endPosTable, final long jdtPos) {
        this.endPosTable = endPosTable;
        this.jdtPos = jdtPos;
    }

    @Override
    public Void scan(Tree tree, Void p) {
        if (tree instanceof JCTree) {
            JCTree jcTree = (JCTree) tree;

            final long jcStartPos = jcTree.getStartPosition();
            final long jcEndPos = jcTree.getEndPosition(endPosTable);

            if (jcStartPos <= jdtPos && jdtPos <= jcEndPos) {
                closestStartPosition = jcStartPos;
                closestEndPosition = jcEndPos;
                tree.accept(this, p);
            }
        }

        return null;
    }
};
