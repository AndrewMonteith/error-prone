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

import java.nio.file.Path;

import static com.google.errorprone.bugtrack.projects.ShouldScanUtils.isJavaFile;
import static com.google.errorprone.bugtrack.projects.ShouldScanUtils.underMain;

public class DubboProject implements CorpusProject {
    @Override
    public String getRoot() {
        return "/home/monty/IdeaProjects/java-corpus/dubbo";
    }

    @Override
    public boolean shouldScanFile(Path file) {
        return isJavaFile(file) && underMain(file);
//        return Files.getFileExtension(file.toString()).equals("java");
    }

    @Override
    public BuildSystem getBuildSystem() {
        return BuildSystem.Maven;
    }
}
