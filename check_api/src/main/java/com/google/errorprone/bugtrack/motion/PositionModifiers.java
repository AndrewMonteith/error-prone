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

package com.google.errorprone.bugtrack.motion;

import com.github.gumtreediff.tree.ITree;
import com.google.errorprone.bugtrack.DatasetDiagnostic;
import com.google.errorprone.bugtrack.motion.trackers.ITreeUtils;
import org.eclipse.jdt.core.dom.ASTNode;

import java.util.Optional;

public final class PositionModifiers {
  private PositionModifiers() {}

  private static long findEndPosOfVariableDecl(ITree root, DatasetDiagnostic diagnostic) {
    return ITreeUtils.getVarDeclNode(root, (int) diagnostic.getStartPos()).getEndPos();
  }

  private static boolean encompassesMultipleVariables(ITree tree, DatasetDiagnostic diagnostic) {
    return ITreeUtils.countNumberOfVariableIdentifiers(tree, (int) diagnostic.getStartPos()) > 1;
  }

  public static DatasetDiagnostic modify(
      SrcFile srcFile, ITree tree, DatasetDiagnostic diagnostic) {
    switch (diagnostic.getType()) {
      case "FieldCanBeFinal":
      case "InitializeInline":
      case "MemberName":
      case "UnusedVariable":
      case "FieldCanBeLocal":
        char pointingAt = srcFile.getChar(diagnostic.getEndPos() - 1);
        return DiagnosticPositionChanger.on(diagnostic)
            .setEndPos(diagnostic.getEndPos() - (pointingAt == ',' || pointingAt == ';' ? 1 : 0))
            .build();
      case "MultiVariableDeclaration":
        return DiagnosticPositionChanger.on(diagnostic)
            .setEndPos(findEndPosOfVariableDecl(tree, diagnostic))
            .build();
      case "RedundantCondition":
        // For a if (<redundant-condition>), it points to the brackets which are not included in the
        // JDT node
        return DiagnosticPositionChanger.on(diagnostic)
            .setStartPos(diagnostic.getStartPos() + 1)
            .setEndPos(diagnostic.getEndPos() - 1)
            .build();
      case "AndroidJdkLibsChecker":
      case "Java7ApiChecker":
        return DiagnosticPositionChanger.on(diagnostic).setPos(diagnostic.getPos() + 1).build();
      default:
        if (diagnostic.getStartPos() != diagnostic.getEndPos()) {
          return diagnostic;
        } else {
          Optional<ITree> highestWithPos =
              ITreeUtils.findHighestNodeWithPos(tree, (int) diagnostic.getStartPos());

          return DiagnosticPositionChanger.on(diagnostic)
              .setEndPos(highestWithPos.map(ITree::getEndPos).orElse((int) diagnostic.getEndPos()))
              .build();
        }
    }
  }

  public static DiagPosEqualityOracle modify(DiagPosEqualityOracle diagPosEqualityOracle) {
    return diagnostic -> {
      switch (diagnostic.getType()) {
        case "FieldCanBeFinal":
        case "InitializeInline":
        case "MemberName":
        case "UnusedVariable":
        case "FieldCanBeLocal":
          return diagPosEqualityOracle.hasSamePosition(diagnostic)
              || diagPosEqualityOracle.hasSamePosition(
                  DiagnosticPositionChanger.on(diagnostic)
                      .setEndPos(diagnostic.getEndPos() + 1)
                      .build());
        default:
          return diagPosEqualityOracle.hasSamePosition(diagnostic);
      }
    };
  }
}
