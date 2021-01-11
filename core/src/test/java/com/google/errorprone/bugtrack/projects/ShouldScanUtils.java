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

package com.google.errorprone.bugtrack.projects;

import com.google.common.io.Files;

import java.nio.file.Path;
import java.util.Set;

public final class ShouldScanUtils {

    public static boolean isJavaFile(Path path) {
       return Files.getFileExtension(path.toString()).equals("java");
    }

    public static boolean underMain(Path path) {
        return !path.toString().contains("/test/");
    }

    public static boolean underDirectory(Path path, String dir) {
        return path.toString().contains("/" + dir + "/");
    }

    public static boolean notInBlockList(Set<String> fileBlockList, Path file) {
        return !fileBlockList.contains(file.getFileName().toString());
    }

}
