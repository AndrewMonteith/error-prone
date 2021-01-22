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
import java.nio.file.Paths;

public final class RootAlternatingProject implements CorpusProject {
    private final CorpusProject project;
    private final Path projDir1, projDir2;

    private boolean returnDir1 = false;

    public RootAlternatingProject(CorpusProject project) {
        this.project = project;
        this.projDir1 = project.getRoot();
        this.projDir2 = Paths.get(project.getRoot() + "_2");
    }

    public void switchDir() {
        this.returnDir1 = !this.returnDir1;
    }

    public Path getOtherDir() {
        if (returnDir1) {
            return projDir2;
        } else {
            return projDir1;
        }
    }

    @Override
    public Path getRoot() {
        if (returnDir1) {
            return projDir1;
        } else {
            return projDir2;
        }
    }

    @Override
    public boolean shouldScanFile(Path file) {
        return project.shouldScanFile(file);
    }

    @Override
    public BuildSystem getBuildSystem() {
        return project.getBuildSystem();
    }
}
