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

import com.github.difflib.algorithm.DiffException;
import com.google.common.collect.ImmutableList;
import com.google.errorprone.bugtrack.ErrorProneInMemoryFileManagerForCheckApi;
import com.google.errorprone.bugtrack.SrcFile;
import com.sun.tools.javac.api.JavacTaskImpl;
import com.sun.tools.javac.api.JavacTool;
import com.sun.tools.javac.util.Context;

import javax.tools.JavaCompiler;
import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;

public final class DPTrackerConstructorFactory {
    private DPTrackerConstructorFactory() {
    }

    public static DiagnosticPositionTrackerConstructor newCharacterLineTracker() {
        return (oldFile, newFile) -> new CharacterLineTracker(oldFile.src, newFile.src);
    }

    public static DiagnosticPositionTrackerConstructor newTokenizedLineTracker() {
        return new TokenizedLineTrackerConstructor();
    }

    private static class TokenizedLineTrackerConstructor implements DiagnosticPositionTrackerConstructor {
        private final ErrorProneInMemoryFileManagerForCheckApi fileManager = new ErrorProneInMemoryFileManagerForCheckApi();

        private final Map<String, Context> contexts;

        public TokenizedLineTrackerConstructor() {
            this.contexts = new HashMap<>();
        }

        private Context getFileContext(String file, List<String> fileSrc) {
            if (contexts.containsKey(file)) {
                return contexts.get(file);
            }

            JavaCompiler javaCompiler = JavacTool.create();
            JavacTaskImpl task =
                    (JavacTaskImpl)
                            javaCompiler.getTask(
                                    new PrintWriter(
                                            new BufferedWriter(new OutputStreamWriter(System.err, UTF_8)), true),
                                    fileManager,
                                    null,
                                    ImmutableList.of("-Xjcov"), // remember end positions of source ranges
                                    null,
                                    ImmutableList.of(fileManager.forSourceLines(file, fileSrc)));

            task.parse();
//        task.analyze();

            contexts.put(file, task.getContext());

            return task.getContext();
        }

        @Override
        public DiagnosticPositionTracker create(SrcFile oldFile, SrcFile newFile) throws DiffException {
            Context oldFileContext = getFileContext(oldFile.name + "_old.java", oldFile.src);
            Context newFileContext = getFileContext(oldFile.name + "_new.java", newFile.src);

            return new TokenizedLineTracker(
                    TokenizedLine.tokenizeSrc(oldFile.src, oldFileContext),
                    TokenizedLine.tokenizeSrc(newFile.src, newFileContext));
        }
    }
}
