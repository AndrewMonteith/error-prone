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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public final class TestFileUtil {
    private static final String TEST_REPO = "src/test/java/com/google/errorprone/bugtrack/testdata/";

    public static List<String> readTestFile(String file) throws IOException {
        return Files.readAllLines(Paths.get(TEST_REPO, file));
    }

    public static SrcFile readTestSrcFile(String file) throws IOException {
        return new SrcFile(file, readTestFile(file));
    }

}
