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

package com.google.errorprone.bugtrack;

import com.google.errorprone.bugtrack.motion.SrcFilePair;
import com.google.errorprone.bugtrack.motion.trackers.*;
import com.google.googlejavaformat.java.FormatterException;
import com.sun.tools.javac.tree.JCTree;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

/**
 * This class is more a collection of regression tests than is completely testing the functionality of the component.
 */
public class JDTToJCPosMapperTest {

    @Test
    public void caseTrackingClassDeclModifiers() throws IOException, FormatterException {
        /*
            JDT positions point to specific modifiers
                Old: points to static
                New: points to public which is before static
            Javac should map it to the ModifiersNode (or relevant class node)
         */

        // GIVEN:
        SrcFilePair srcFilePair = TestUtils.readTestSrcFilePair(
                "breaking_changes/start_node_changing_old.java",
                "breaking_changes/start_node_changing_new.java");

        final long newNodePos = 4989; // position of static keyword in new file
        JCTree.JCCompilationUnit compUnit = new TrackersSharedState().loadNewJavacAST(srcFilePair);

        // WHEN:
        JDTToJCPosMapper mapper = new JDTToJCPosMapper(compUnit.endPositions, newNodePos);
        mapper.scan(compUnit, null);

        // THEN:
        Assert.assertEquals(4982, mapper.getClosestStartPosition());
    }

    @Test
    public void caseMappingVariableNames() throws IOException, FormatterException {
        // Variables are represented by JcVariableDecl which has no children. Only the modifiers are visited
        // as a nested child. Hence positions are mapped to the start of the JcVariableDecl node.

        // GIVEN:
        SrcFilePair srcFilePair = TestUtils.readTestSrcFilePair(
                "breaking_changes/start_node_changing_old.java",
                "breaking_changes/start_node_changing_new.java");

        final long newNodePos = 2056; // start position of 'constructionProxy' on line 55
        JCTree.JCCompilationUnit compUnit = new TrackersSharedState().loadNewJavacAST(srcFilePair);

        // WHEN:
        JDTToJCPosMapper mapper = new JDTToJCPosMapper(compUnit.endPositions, newNodePos);
        mapper.scan(compUnit, null);

        // THEN:
        Assert.assertEquals(2030, mapper.getClosestStartPosition());
    }

    @Test
    public void caseTrackingParameterIdentifier() throws IOException, FormatterException {
        // Java method parameters are mapped to JCVariableDecl so similar case to variable declarations.
        // Positions from the JDT tree are mapped to positions at the start of the VariableDeclarations

        // GIVEN:
        SrcFilePair srcFilePair = TestUtils.readTestSrcFilePair(
                "breaking_changes/start_node_changing_old.java",
                "breaking_changes/start_node_changing_new.java");

        final long newNodePos = 2515; // start position of 'methodInvocation' on line 76
        JCTree.JCCompilationUnit compUnit = new TrackersSharedState().loadNewJavacAST(srcFilePair);

        // WHEN:
        JDTToJCPosMapper mapper = new JDTToJCPosMapper(compUnit.endPositions, newNodePos);
        mapper.scan(compUnit, null);

        // THEN:
        Assert.assertEquals(2498, mapper.getClosestStartPosition());
    }

    @Test
    public void caseTrackingMethodReturnType() throws IOException, FormatterException {
        // Return types are their own node in the AST. This makes relatively easy translation between JDT and JC

        // GIVEN:
        SrcFilePair srcFilePair = TestUtils.readTestSrcFilePair(
                "breaking_changes/start_node_changing_old.java",
                "breaking_changes/start_node_changing_new.java");

        final long newNodePos = 2484; // start position of 'Object' on line 76
        JCTree.JCCompilationUnit compUnit = new TrackersSharedState().loadNewJavacAST(srcFilePair);

        // WHEN:
        JDTToJCPosMapper mapper = new JDTToJCPosMapper(compUnit.endPositions, newNodePos);
        mapper.scan(compUnit, null);

        // THEN:
        Assert.assertEquals(2484, mapper.getClosestStartPosition());

    }
}
