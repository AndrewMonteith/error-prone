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

import com.github.gumtreediff.gen.jdt.JdtVisitor;
import com.github.gumtreediff.gen.jdt.cd.EntityType;
import com.google.common.collect.ImmutableList;
import com.google.errorprone.bugtrack.motion.SrcFile;
import org.eclipse.jdt.core.dom.*;

import java.util.regex.Pattern;

public class BetterJdtVisitor extends JdtVisitor {
  private static final Pattern X_DOT_Y = Pattern.compile("^[a-zA-Z_]\\w*\\.[a-zA-Z_]\\w*$");

  private final SrcFile srcFile;

  public BetterJdtVisitor(SrcFile srcFile) {
    this.srcFile = srcFile;
  }

  @Override
  protected String getLabel(ASTNode node) {
    if (node instanceof ImportDeclaration) {
      ImportDeclaration importDecl = (ImportDeclaration) node;
      String labelPrefix = importDecl.isStatic() ? "static " : "";
      return labelPrefix + importDecl.getName().getFullyQualifiedName();
    } else {
      return super.getLabel(node);
    }
  }

  @Override
  public boolean visit(TypeLiteral typeLiteral) {
    Type type = typeLiteral.getType();
    pushNode(type, type.toString());
    popNode();

    pushFakeNode(
        EntityType.SIMPLE_NAME,
        type.getStartPosition() + type.toString().length() + 1,
        type.toString().length());
    getCurrentParent().setLabel("class");

    popNode();
    return false;
  }

  @Override
  public boolean visit(QualifiedName qualName) {
    if (X_DOT_Y.matcher(qualName.getFullyQualifiedName()).matches()) {
      pushNode(qualName.getQualifier(), qualName.getQualifier().toString());
      popNode();
      pushNode(qualName.getName(), qualName.getName().toString());
      popNode();
    }

    return false;
  }

  @Override
  public boolean visit(TryStatement tryStatement) {
    pushFakeNode(EntityType.SIMPLE_NAME, tryStatement.getStartPosition(), 3);
    getCurrentParent().setLabel("try-sig");

    pushFakeNode(EntityType.SIMPLE_NAME, tryStatement.getStartPosition(), 3);
    getCurrentParent().setLabel("try-sig");
    popNode();

    pushFakeNode(EntityType.SIMPLE_NAME, tryStatement.getStartPosition(), 4);
    getCurrentParent().setLabel("try-sig");
    popNode();

    popNode();

    return super.visit(tryStatement);
  }

  private long findStartOfTypeDeclToken(final long start, String token) {
    // start can point to either the beginning of a comment, modifiers, or the actual token
    ImmutableList<String> lines = srcFile.getLines();
    int lineIndex = (int) srcFile.getLineNumber(start) - 1;

    // skip javadoc is present
    if (lines.get(lineIndex).trim().startsWith("/*")) {
      while (!lines.get(lineIndex).trim().endsWith("*/")) {
        ++lineIndex;
      }
      ++lineIndex;
    }

    while (lineIndex < lines.size() && !lines.get(lineIndex).contains(token)) {
      ++lineIndex;
    }

    if (lineIndex == lines.size()) {
      throw new RuntimeException(
          "requested wrong token type." + srcFile.getName() + " " + start + " " + token);
    }

    // we should now be on the <modifiers> <token> <identifier> {
    final int column = lines.get(lineIndex).indexOf(token) + 1;
    return srcFile.getPosition((lineIndex + 1), column);
  }

  private int findOperationStartPos(Assignment assignment) {
    // Work backwards from start of assignment RHS until we find a '='
    int rhsStartPos = assignment.getRightHandSide().getStartPosition();

    while (!srcFile.getSrcExtract(rhsStartPos, rhsStartPos + 1).equals("=")) {
      --rhsStartPos;
    }

    return rhsStartPos - 1;
  }

  @Override
  public boolean visit(Assignment assignment) {
    // Added because of
    // b4179cb9a615d5996a7321b78869d1c731e159fa 1
    // rds/user/am2857/hpc-work/java-corpus/9674/src/main/java/com/gmail/nossr50/datatypes/skills/subskills/acrobatics/Roll.java 354 10 12413 12416 12476
    // [NarrowingCompoundAssignment] Compound assignments from double to float hide lossy casts
    // (see https://errorprone.info/bugpattern/NarrowingCompoundAssignment)
    // Did you mean 'xp = (float) (xp *ExperienceConfig.getInstance().getFeatherFallXPModifier());'?

    String op = assignment.getOperator().toString();
    if (op.length() > 1) {
      // *=, +=, /=, ...
      final long opStartPos = findOperationStartPos(assignment);
      pushFakeNode(EntityType.SIMPLE_NAME, (int) opStartPos, op.length());
      getCurrentParent().setLabel(op);
      popNode();
    }
    return super.visit(assignment);
  }

  @Override
  public boolean visit(TypeDeclaration typeDecl) {
    //        Added for guice since position points to class keyword which was not included in the
    // AST
    //        a533bf26c612003a99996f07f64148ddd1602d06 1
    //        ----DIAGNOSTIC
    //
    // /rds/user/am2857/hpc-work/java-corpus/16933/core/test/com/google/inject/internal/ProxyFactoryTest.java 159 10 4705 4712 4825
    //        [ClassNamedLikeTypeParameter] This class's name looks like a Type Parameter.
    //        (see https://errorprone.info/bugpattern/ClassNamedLikeTypeParameter)
    //        ffb154d0304e7c226e1495e5bf0344ff9313a29c 1
    //        ----DIAGNOSTIC
    //
    // /rds/user/am2857/hpc-work/java-corpus/16933/core/test/com/google/inject/internal/ProxyFactoryTest.java 159 17 4756 4770 4883
    //        [ClassNamedLikeTypeParameter] This class's name looks like a Type Parameter.
    //        (see https://errorprone.info/bugpattern/ClassNamedLikeTypeParameter)
    String label = typeDecl.isInterface() ? "interface" : "class";
    final long labelStartPos = findStartOfTypeDeclToken(typeDecl.getStartPosition(), label);

    pushFakeNode(EntityType.SIMPLE_NAME, (int) labelStartPos, label.length());
    getCurrentParent().setLabel(label);
    popNode();

    return super.visit(typeDecl);
  }
}
