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
import org.eclipse.jdt.core.dom.*;

import java.util.regex.Pattern;

public class StaticImportPreservingJdtVisitor extends JdtVisitor {
    private static final Pattern X_DOT_Y = Pattern.compile("^[a-zA-Z_]\\w*\\.[a-zA-Z_]\\w*$");

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

}
