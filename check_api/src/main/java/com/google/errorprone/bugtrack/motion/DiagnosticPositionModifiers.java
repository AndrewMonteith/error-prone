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

import java.util.Optional;

public final class DiagnosticPositionModifiers {
  private DiagnosticPositionModifiers() {}

  private static long findEndPosOfVariableDecl(ITree root, DatasetDiagnostic diagnostic) {
    return ITreeUtils.getVarDeclNode(root, (int) diagnostic.getStartPos()).getEndPos();
  }

  private static boolean encompassesMultipleVariables(ITree tree, DatasetDiagnostic diagnostic) {
    return ITreeUtils.countNumberOfVariableIdentifiers(tree, (int) diagnostic.getStartPos()) > 1;
  }

  public static DatasetDiagnostic modify(ITree tree, DatasetDiagnostic diagnostic) {
    switch (diagnostic.getType()) {
      case "MultiVariableDeclaration":
        return PositionChanger.on(diagnostic)
            .setEndPos(findEndPosOfVariableDecl(tree, diagnostic))
            .build();
      case "FieldCanBeFinal":
      case "InitializeInline":
      case "MemberName":
      case "UnusedVariable":
        return PositionChanger.on(diagnostic)
            .setEndPos(
                diagnostic.getEndPos() - (encompassesMultipleVariables(tree, diagnostic) ? 1 : 0))
            .build();
      case "AndroidJdkLibsChecker":
      case "Java7ApiChecker":
        return PositionChanger.on(diagnostic).setPos(diagnostic.getPos() + 1).build();
      case "MissingSummary":
      case "InvalidLink":
        if (diagnostic.getStartPos() != diagnostic.getEndPos()) {
          return diagnostic;
        } else {
          return PositionChanger.on(diagnostic)
              .setEndPos(
                  ITreeUtils.findHighestNodeWithPos(tree, (int) diagnostic.getStartPos())
                      .get()
                      .getEndPos())
              .build();
        }
      default:
        return diagnostic;
    }
  }

  private static class PositionChanger {
    private final DatasetDiagnostic originalDiagnostic;
    private Optional<Long> startPos = Optional.empty();
    private Optional<Long> pos = Optional.empty();
    private Optional<Long> endPos = Optional.empty();

    PositionChanger(DatasetDiagnostic diagnostic) {
      this.originalDiagnostic = diagnostic;
    }

    static PositionChanger on(DatasetDiagnostic diagnostic) {
      return new PositionChanger(diagnostic);
    }

    public DatasetDiagnostic build() {
      return new DatasetDiagnostic(
          originalDiagnostic.getFileName(),
          originalDiagnostic.getLineNumber(),
          originalDiagnostic.getColumnNumber(),
          startPos.orElse(originalDiagnostic.getStartPos()),
          pos.orElse(originalDiagnostic.getPos()),
          endPos.orElse(originalDiagnostic.getEndPos()),
          originalDiagnostic.getMessage());
    }

    PositionChanger setStartPos(final long pos) {
      this.startPos = Optional.of(pos);
      return this;
    }

    PositionChanger setPos(final long pos) {
      this.pos = Optional.of(pos);
      return this;
    }

    PositionChanger setEndPos(final long pos) {
      this.endPos = Optional.of(pos);
      return this;
    }
  }
}
