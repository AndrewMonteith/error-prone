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

import com.google.common.collect.ImmutableList;
import com.google.errorprone.bugtrack.utils.ProjectFiles;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static com.google.errorprone.bugtrack.projects.ShouldScanUtils.*;

/**
 * This one takes a while to scan.
 */
public final class HibernateProject implements CorpusProject {
    @Override
    public Path getRoot() {
        return ProjectFiles.get("java-corpus/hibernate-orm");
    }

    private static final List<String> allowList = ImmutableList.of("src/main", "target/generated-src");

    @Override
    public boolean shouldScanFile(Path file) {
        return isJavaFile(file) && inDirectoryAllowlist(allowList, file) && !underDirectory(file, "documentation");
    }

    @Override
    public BuildSystem getBuildSystem() {
        return BuildSystem.Gradle;
    }
}
