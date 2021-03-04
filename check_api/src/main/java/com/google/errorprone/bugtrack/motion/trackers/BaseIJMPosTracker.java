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
import com.google.errorprone.bugtrack.utils.IOThrowingSupplier;
import com.sun.tools.javac.tree.JCTree;

import java.io.IOException;
import java.util.Optional;

public abstract class BaseIJMPosTracker {
    private final TrackersSharedState sharedState;
    private final SrcFilePair srcFilePair;

    private final TreeContext oldSrcTree;
    private final TreeContext newSrcTree;
    private final MappingStore mappings;

    protected BaseIJMPosTracker(SrcFilePair srcFilePair,
                                TrackersSharedState sharedState,
                                IOThrowingSupplier<AbstractJdtVisitor> jdtVisitorSupplier) throws IOException {
        this.sharedState = sharedState;
        this.srcFilePair = srcFilePair;
        this.oldSrcTree = parseFileWithVisitor(srcFilePair.oldFile, jdtVisitorSupplier.get());
        this.newSrcTree = parseFileWithVisitor(srcFilePair.newFile, jdtVisitorSupplier.get());
        this.mappings = new MappingStore();

        new JavaMatchers.IterativeJavaMatcher_V2(
                oldSrcTree.getRoot(), newSrcTree.getRoot(), mappings).match();
    }


    public BaseIJMPosTracker(SrcFilePair srcFilePair, TrackersSharedState sharedState) throws IOException {
        this(srcFilePair, sharedState, StaticImportPreservingJdtVisitor::new);
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

    protected Optional<NodeLocation> findClosestMatchingSrcBuffer(final long pos) {
        Optional<ITree> closestOldJdtNode = findClosestMatchingJDTNode(pos);
        if (!closestOldJdtNode.isPresent()) {
            return Optional.empty();
        }

        // Find which JDT that maps to in the new AST
        ITree newJdtNode = mappings.getDst(closestOldJdtNode.get());

        // Find the src buffer range of the closest JC node to matched jdt node's start position
        JCTree.JCCompilationUnit newJCAst = sharedState.loadNewJavacAST(srcFilePair);
        JDTToJCPosMapper startPosMapper = new JDTToJCPosMapper(newJCAst.endPositions, newJdtNode.getPos());
        startPosMapper.scan(newJCAst, null);

        JDTToJCPosMapper endPosMapper = new JDTToJCPosMapper(newJCAst.endPositions, newJdtNode.getEndPos());
        endPosMapper.scan(newJCAst, null);

        return Optional.of(new NodeLocation(
                startPosMapper.getClosestStartPosition(),
                startPosMapper.getClosestPreferredPosition(),
                endPosMapper.getClosestEndPosition()));
    }

    private static TreeContext parseFileWithVisitor(SrcFile file, AbstractJdtVisitor visitor) throws IOException {
        String src = file.getSrc();

        AbstractJdtTreeGenerator treeGenerator = new AbstractJdtTreeGenerator() {
            @Override
            protected AbstractJdtVisitor createVisitor() {
                return visitor;
            }
        };

        return treeGenerator.generateFromString(src);
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
