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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.errorprone.bugtrack.ErrorProneInMemoryFileManagerForCheckApi;
import com.google.errorprone.bugtrack.motion.SrcFile;
import com.google.errorprone.bugtrack.motion.SrcFilePair;
import com.google.errorprone.bugtrack.utils.MemoMap;
import com.sun.tools.javac.api.JavacTaskImpl;
import com.sun.tools.javac.api.JavacTool;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Pair;

import javax.tools.JavaCompiler;
import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;

public final class TrackersSharedState {
    private final MemoMap<String, Pair<JCTree.JCCompilationUnit, Context>> javacAstsAndContexts;

    private final JavaCompiler javacCompiler;
    private final ErrorProneInMemoryFileManagerForCheckApi fileManager;

    public TrackersSharedState() {
        this.fileManager = new ErrorProneInMemoryFileManagerForCheckApi();
        this.javacCompiler = JavacTool.create();
        this.javacAstsAndContexts = new MemoMap<>();
    }

    private Pair<JCTree.JCCompilationUnit, Context> parseFileWithJavac(String fileName, List<String> fileSrc) {
        JavacTaskImpl task =
                (JavacTaskImpl)
                        javacCompiler.getTask(
                                new PrintWriter(
                                        new BufferedWriter(new OutputStreamWriter(System.err, UTF_8)), true),
                                fileManager,
                                null,
                                ImmutableList.of("-Xjcov"), // remember end positions of source ranges
                                null,
                                ImmutableList.of(fileManager.forSourceLines(fileName, fileSrc)));

        JCTree.JCCompilationUnit tree = (JCTree.JCCompilationUnit) Iterables.getFirst(task.parse(), null);

        if (tree == null) {
            throw new RuntimeException("failed to parse ast for " + fileName);
        }

        return new Pair<>(tree, task.getContext());
    }

    private Pair<JCTree.JCCompilationUnit, Context> loadJavacInfo(SrcFile file, String suffixId) {
        String fileNameId = file.getName() + suffixId;
        return javacAstsAndContexts.getOrInsert(fileNameId, () -> parseFileWithJavac(fileNameId, file.getLines()));
    }

    public JCTree.JCCompilationUnit loadOldJavacAST(SrcFilePair srcFilePair) {
        return loadJavacInfo(srcFilePair.oldFile, "_old.java").fst;
    }

    public Context loadOldJavacContext(SrcFilePair srcFilePair) {
        return loadJavacInfo(srcFilePair.oldFile, "_old.java").snd;
    }

    public JCTree.JCCompilationUnit loadNewJavacAST(SrcFilePair srcFilePair) {
        return loadJavacInfo(srcFilePair.newFile, "_new.java").fst;
    }

    public Context loadNewJavacContext(SrcFilePair srcFilePair) {
        return loadJavacInfo(srcFilePair.newFile, "_new.java").snd;
    }
}
