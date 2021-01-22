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

import java.io.File;
import java.nio.file.Path;

public final class ProjectFile {
    private final CorpusProject project;
    private final Path projPath;

    public ProjectFile(CorpusProject project, Path path) {
        if (!path.startsWith(project.getRoot())) {
            throw new IllegalArgumentException(path.toString() + " not in " + project.getRoot());
        }

        this.project = project;
        this.projPath = project.getRoot().relativize(path);
    }

    public boolean exists() {
        return toFile().exists();
    }

    public File toFile() {
        return project.getRoot().resolve(projPath).toFile();
    }

    @Override
    public String toString() {
        return toFile().toString();
    }
}
