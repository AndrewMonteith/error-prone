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
import com.google.errorprone.bugtrack.ErrorProneInMemoryFileManagerForCheckApi;
import com.google.errorprone.bugtrack.motion.DiagPosEqualityOracle;
import com.sun.tools.javac.api.JavacTaskImpl;
import com.sun.tools.javac.api.JavacTool;
import com.sun.tools.javac.util.Context;

import javax.tools.JavaCompiler;
import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.*;

import static java.nio.charset.StandardCharsets.UTF_8;

public final class DPTrackerConstructorFactory {
    private DPTrackerConstructorFactory() {
    }

    public static DiagnosticPositionTrackerConstructor newCharacterLineTracker() {
        return srcFilePair -> new CharacterLineTracker(srcFilePair.oldFile.getLines(), srcFilePair.newFile.getLines());
    }

    public static DiagnosticPositionTrackerConstructor newTokenizedLineTracker() {
        final JavacFileContextManager contextManager = new JavacFileContextManager();

        return srcFilePair -> {
            Context oldFileContext = contextManager.getFileContext(srcFilePair.oldFile.getName() + "_old.java", srcFilePair.oldFile.getLines());
            Context newFileContext = contextManager.getFileContext(srcFilePair.newFile.getName() + "_new.java", srcFilePair.newFile.getLines());

            return new TokenizedLineTracker(
                    TokenizedLine.tokenizeSrc(srcFilePair.oldFile.getLines(), oldFileContext),
                    TokenizedLine.tokenizeSrc(srcFilePair.newFile.getLines(), newFileContext));
        };
    }

    public static DiagnosticPositionTrackerConstructor newIJMAstNodeTracker() {
        return IJMAstNodeTracker::new;
    }

    public static DiagnosticPositionTrackerConstructor pipeline(DiagnosticPositionTrackerConstructor... trackerCtors) {
        return srcFilePair -> {
            List<DiagnosticPositionTracker> trackers = new ArrayList<>();
            for (DiagnosticPositionTrackerConstructor trackerCtor : trackerCtors) {
                trackers.add(trackerCtor.create(srcFilePair));
            }

            return diagnostic -> {
                for (DiagnosticPositionTracker tracker : trackers) {
                    Optional<DiagPosEqualityOracle> posEqOracle = tracker.track(diagnostic);
                    if (posEqOracle.isPresent()) {
                        return posEqOracle;
                    }
                }

                return Optional.empty();
            };
        };
    }

    private static class JavacFileContextManager {
        private final ErrorProneInMemoryFileManagerForCheckApi fileManager = new ErrorProneInMemoryFileManagerForCheckApi();

        private final Map<String, Context> contexts;

        public JavacFileContextManager() {
            this.contexts = new HashMap<>();
        }

        public Context getFileContext(String file, List<String> fileSrc) {
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
    }
}
