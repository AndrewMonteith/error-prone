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

import at.aau.softwaredynamics.matchers.JavaMatchers;
import com.github.gumtreediff.gen.jdt.AbstractJdtTreeGenerator;
import com.github.gumtreediff.gen.jdt.AbstractJdtVisitor;
import com.github.gumtreediff.matchers.MappingStore;
import com.github.gumtreediff.tree.ITree;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.errorprone.bugtrack.motion.SrcFile;
import com.google.errorprone.bugtrack.motion.SrcFilePair;
import com.google.errorprone.bugtrack.motion.trackers.BetterJdtVisitor;
import com.sun.tools.javac.api.JavacTaskImpl;
import com.sun.tools.javac.api.JavacTool;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Pair;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;

public final class SrcPairInfo {
  public final SrcFilePair files;
  private final ErrorProneInMemoryFileManagerForCheckApi fileManager;
  private final Lazy<Pair<JCTree.JCCompilationUnit, Context>> oldAstAndContext;
  private final Lazy<Pair<JCTree.JCCompilationUnit, Context>> newAstAndContext;
  private final Lazy<ITree> oldJdtTree;
  private final Lazy<ITree> newJdtTree;
  private final Lazy<MappingStore> mappings;

  public SrcPairInfo(SrcFilePair files) {
    this.files = files;
    this.fileManager = new ErrorProneInMemoryFileManagerForCheckApi();
    this.oldAstAndContext = new Lazy<>(() -> loadJavacInfo(files.oldFile, "_old.java"));
    this.newAstAndContext = new Lazy<>(() -> loadJavacInfo(files.newFile, "_new.java"));
    this.oldJdtTree = new Lazy<>(() -> loadJdtTree(files.oldFile));
    this.newJdtTree = new Lazy<>(() -> loadJdtTree(files.newFile));
    this.mappings = new Lazy<>(() -> {
      MappingStore store = new MappingStore();
      new JavaMatchers.IterativeJavaMatcher_V2(oldJdtTree.get(), newJdtTree.get(), store).match();;
      return store;
    });
  }

  private static ITree loadJdtTree(SrcFile file) throws IOException {
    System.out.println("Loaded tree for " + file.getName());
    String src = file.getSrc();

    AbstractJdtTreeGenerator treeGenerator =
        new AbstractJdtTreeGenerator() {
          @Override
          protected AbstractJdtVisitor createVisitor() {
            return new BetterJdtVisitor(file);
          }
        };

    return treeGenerator.generateFromString(src).getRoot();
  }

  private Pair<JCTree.JCCompilationUnit, Context> parseFileWithJavac(
      String fileName, List<String> fileSrc) {
    JavacTaskImpl task =
        (JavacTaskImpl)
            JavacTool.create()
                .getTask(
                    new PrintWriter(
                        new BufferedWriter(new OutputStreamWriter(System.err, UTF_8)), true),
                    fileManager,
                    null,
                    ImmutableList.of("-Xjcov"), // remember end positions of source ranges
                    null,
                    ImmutableList.of(fileManager.forSourceLines(fileName, fileSrc)));

    JCTree.JCCompilationUnit tree =
        (JCTree.JCCompilationUnit) Iterables.getFirst(task.parse(), null);

    if (tree == null) {
      throw new RuntimeException("failed to parse ast for " + fileName);
    }

    return new Pair<>(tree, task.getContext());
  }

  private Pair<JCTree.JCCompilationUnit, Context> loadJavacInfo(SrcFile file, String suffixId) {
    String fileNameId = file.getName() + suffixId;
    return parseFileWithJavac(fileNameId, file.getLines());
  }

  public JCTree.JCCompilationUnit loadOldJavacAST() {
    return oldAstAndContext.get().fst;
  }

  public Context loadOldJavacContext() {
    return oldAstAndContext.get().snd;
  }

  public JCTree.JCCompilationUnit loadNewJavacAST() {
    return newAstAndContext.get().fst;
  }

  public Context loadNewJavacContext() {
    return newAstAndContext.get().snd;
  }

  public ITree getOldJdtTree() {
    return oldJdtTree.get();
  }

  public ITree getMatchedOldJdtTree() {
    mappings.get();
    return oldJdtTree.get();
  }

  public ITree getNewJdtTree() {
    return newJdtTree.get();
  }

  public ITree getMatchedNewJdtTree() {
    mappings.get();
    return newJdtTree.get();
  }

  public ITree getMatch(ITree oldNode) {
    return mappings.get().getDst(oldNode);
  }
}
