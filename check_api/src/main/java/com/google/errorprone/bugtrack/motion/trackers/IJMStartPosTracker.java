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
import com.github.gumtreediff.matchers.MappingStore;
import com.github.gumtreediff.tree.ITree;
import com.github.gumtreediff.tree.TreeContext;
import com.google.errorprone.bugtrack.DatasetDiagnostic;
import com.google.errorprone.bugtrack.motion.DiagPosEqualityOracle;
import com.google.errorprone.bugtrack.motion.SrcFilePair;

import java.io.IOException;
import java.util.Optional;

public final class IJMStartPosTracker implements DiagnosticPositionTracker {
    private final TrackersSharedState sharedState;
    private final SrcFilePair srcFilePair;

    private final TreeContext oldSrcTree;
    private final TreeContext newSrcTree;
    private final MappingStore mappings;

    public IJMStartPosTracker(SrcFilePair srcFilePair, TrackersSharedState sharedState) throws IOException {
        this.sharedState = sharedState;
        this.srcFilePair = srcFilePair;
        this.oldSrcTree = KeepEveryNodeTreeGenerator.parseSrc(srcFilePair.oldFile);
        this.newSrcTree = KeepEveryNodeTreeGenerator.parseSrc(srcFilePair.newFile);
        this.mappings = new MappingStore();

        new JavaMatchers.IterativeJavaMatcher_V2(
                oldSrcTree.getRoot(), newSrcTree.getRoot(), mappings).match();
    }

    protected Optional<ITree> findClosestJDTNode(final long pos) {
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

    private long findClosestJCNodeToPos(final long pos) {
        JDTToJCStartPosMapper mapper = new JDTToJCStartPosMapper(sharedState.loadNewJavacAST(srcFilePair).endPositions, pos);
        mapper.scan(sharedState.loadNewJavacAST(srcFilePair), null);
        return mapper.getClosestStartPosition();
    }

    @Override
    public Optional<DiagPosEqualityOracle> track(DatasetDiagnostic oldDiag) {
        // Find closest JDT node to the start position of the diagnostic in the old AST
        Optional<ITree> closestOldJdtNode = findClosestJDTNode(oldDiag.getStartPos());
        if (!closestOldJdtNode.isPresent()) {
            return Optional.empty();
        }

        // Find which JDT that maps to in the new AST
        ITree newJdtNode = mappings.getDst(closestOldJdtNode.get());

        // Find the closest JC node to new jdt node's start position
        final long newJCStartPos = findClosestJCNodeToPos(newJdtNode.getPos());

        return Optional.of(DiagPosEqualityOracle.byStartPos(newJCStartPos));
    }
}

