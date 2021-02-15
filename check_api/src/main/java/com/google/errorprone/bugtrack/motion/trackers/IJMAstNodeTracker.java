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
import com.github.gumtreediff.gen.jdt.JdtVisitor;
import com.github.gumtreediff.matchers.MappingStore;
import com.github.gumtreediff.tree.ITree;
import com.github.gumtreediff.tree.TreeContext;
import com.google.errorprone.bugtrack.DatasetDiagnostic;
import com.google.errorprone.bugtrack.motion.DiagPosEqualityOracle;
import com.google.errorprone.bugtrack.motion.DiagnosticsDeltaManager;
import com.google.errorprone.bugtrack.motion.SrcFile;

import java.io.IOException;
import java.util.Optional;

public final class IJMAstNodeTracker implements DiagnosticPositionTracker {
    private final SrcFile oldSrcFile;
    private final SrcFile newSrcFile;

    private final TreeContext oldSrcTree;
    private final TreeContext newSrcTree;

    private final MappingStore mappings;

    public IJMAstNodeTracker(DiagnosticsDeltaManager.SrcFilePair srcFilePair) throws IOException {
        this.oldSrcFile = srcFilePair.oldFile;
        this.newSrcFile = srcFilePair.newFile;
        this.oldSrcTree = KeepEveryNodeTreeGenerator.parseSrc(srcFilePair.oldFile);
        this.newSrcTree = KeepEveryNodeTreeGenerator.parseSrc(srcFilePair.newFile);
        this.mappings = new MappingStore();

        new JavaMatchers.IterativeJavaMatcher_V2(
                oldSrcTree.getRoot(), newSrcTree.getRoot(), mappings).match();
    }

    private Optional<ITree> findClosestNodeThatMatches(final long pos) {
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

    @Override
    public Optional<DiagPosEqualityOracle> track(DatasetDiagnostic diag) {
        return findClosestNodeThatMatches(diag.getStartPos()).map(
                oldNode -> DiagPosEqualityOracle.byStartPos(mappings.getDst(oldNode).getPos()));
    }
}

class KeepEveryNodeTreeGenerator extends AbstractJdtTreeGenerator {
    @Override
    protected AbstractJdtVisitor createVisitor() {
        return new JdtVisitor();
    }

    static TreeContext parseSrc(SrcFile file) throws IOException {
        return new KeepEveryNodeTreeGenerator().generateFromString(file.getSrc());
    }
}
