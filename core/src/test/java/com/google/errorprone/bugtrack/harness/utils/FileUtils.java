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

package com.google.errorprone.bugtrack.harness.utils;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public final class FileUtils {

    public static List<Path> findFilesMatchingGlob(Path root, String glob) {
        final PathMatcher pathMatcher = FileSystems.getDefault().getPathMatcher("glob:" + glob);
        List<Path> result = new ArrayList<>();

        try {
            Files.walkFileTree(root, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path path,
                                                 BasicFileAttributes attrs) throws IOException {
                    if (pathMatcher.matches(path)) {
                        result.add(path);
                    }

                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }

        return result;
    }

}
