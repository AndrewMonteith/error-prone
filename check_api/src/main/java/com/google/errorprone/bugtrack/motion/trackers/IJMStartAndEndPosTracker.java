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
import com.google.common.collect.Iterables;
import com.google.errorprone.bugtrack.DatasetDiagnostic;
import com.google.errorprone.bugtrack.SrcPairInfo;
import com.google.errorprone.bugtrack.motion.DiagPosEqualityOracle;
import com.google.errorprone.bugtrack.motion.DiagSrcPosEqualityOracle;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class IJMStartAndEndPosTracker implements DiagnosticPositionTracker {
  private final SrcPairInfo srcPairInfo;

  public IJMStartAndEndPosTracker(SrcPairInfo srcPairInfo) {
    this.srcPairInfo = srcPairInfo;
  }

  private static boolean isTemplatedClassNode(ITree tree) {
    if (tree.getLabel().isEmpty()) {
      return false;
    }

    String className = tree.getLabel();
    String templatedClassName = className + "<";
    while (tree != null && tree.getLabel().startsWith(className)) {
      if (tree.getLabel().startsWith(templatedClassName)) {
        return true;
      }

      tree = tree.getParent();
    }

    return false;
  }

  @Override
  public boolean shouldAdjustPositions(DatasetDiagnostic oldDiagnostic) {
    return true;
  }

  @Override
  public Optional<DiagPosEqualityOracle> track(DatasetDiagnostic oldDiag) {
    Optional<NodeLocation> newStartLoc = trackStartNodePosition(oldDiag.getStartPos());
    if (!newStartLoc.isPresent()) {
      return Optional.empty();
    }

    Optional<List<NodeLocation>> endLocs = trackEndPosition(oldDiag);
    if (!endLocs.isPresent() || endLocs.get().isEmpty()) {
      return Optional.empty();
    }

    final long newStartPos = newStartLoc.get().startPos;
    return endLocs.map(
        locs ->
            DiagPosEqualityOracle.any(
                Iterables.transform(
                    locs,
                    endLoc ->
                        DiagSrcPosEqualityOracle.byStartAndEndPos(newStartPos, endLoc.endPos))));
  }

  private ITree findNodeWithAngleBrackets(ITree tree) {
    ITree node = tree;
    while (node != null && !node.getLabel().endsWith(">")) {
      node = node.getParent();
    }

    if (node == null) {
      String msg =
          "class began with < but couldn't find >\n"
              + "Tree label "
              + tree.getLabel()
              + "\n"
              + "Old file "
              + srcPairInfo.files.oldFile.getName()
              + "\n"
              + "New file "
              + srcPairInfo.files.newFile.getName()
              + "\n";

      throw new RuntimeException(msg);
    }

    return node;
  }

  private NodeLocation mapJdtSrcRangeToJCSrcRange(ITree jdtNode) {
    // Find the src buffer range of the closest JC node to matched jdt node's start position
    return new JDTToJCMapper(srcPairInfo.loadNewJavacAST()).map(jdtNode);
  }

  private Optional<NodeLocation> trackStartNodePosition(final long startPos) {
    return ITreeUtils.findHighestMatchedNodeThat(
            srcPairInfo.getMatchedOldJdtTree(), node -> node.getPos() == startPos)
        .map(
            closestOldJdtNode ->
                mapJdtSrcRangeToJCSrcRange(srcPairInfo.getMatch(closestOldJdtNode)));
  }

  private Optional<List<NodeLocation>> trackEndPosition(DatasetDiagnostic diagnostic) {
    // We return a list since we're going to consider multiple cases for a possible end position
    // If the position refers to a non-generic class (determined syntatically) then we merely return
    // one location by tracking the token. Else we track both the token and the 'token<...>'
    //    final long endPos = modifyEndPosition(diagnostic);

    return ITreeUtils.findHighestMatchedNodeThat(
            srcPairInfo.getMatchedOldJdtTree(), node -> node.getEndPos() == diagnostic.getEndPos())
        .map(
            closestOldJdtNode -> {
              ITree newJdtNode = srcPairInfo.getMatch(closestOldJdtNode);

              List<NodeLocation> locations = new ArrayList<>();
              if (isTemplatedClassNode(newJdtNode)) {
                locations.add(mapJdtSrcRangeToJCSrcRange(findNodeWithAngleBrackets(newJdtNode)));
              }

              locations.add(mapJdtSrcRangeToJCSrcRange(newJdtNode));

              return locations;
            });
  }
}
