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

  //    @Override
  //    public boolean visit(TypeDeclaration typeDecl) {
  //        String label = typeDecl.isInterface() ? "interface" : "class";
  //        // No easy way to find the source region of the 'interface' or 'class' label
  //        List<IExtendedModifier> modifiers = (List<IExtendedModifier>) typeDecl.modifiers();
  //
  //
  //    }

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

    // we should now be on the <modifiers> <token> <identifier> line
    final int column = lines.get(lineIndex).indexOf(token) + 1;
    if (column == 0) {
      throw new RuntimeException(
          "requested wrong token type."
              + srcFile.getName()
              + " "
              + (lineIndex + 1)
              + " "
              + lines.get(lineIndex)
              + " "
              + token);
    }

    return srcFile.getPosition((lineIndex + 1), column);
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
