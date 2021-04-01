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
import java.nio.file.Paths;
import java.util.Objects;

public final class ProjectFile {
  private final CorpusProject project;
  private final Path projPath;

  public ProjectFile(CorpusProject project, Path path) {
    this.project = project;

    if (path.startsWith(project.getRoot())) {
      this.projPath = project.getRoot().relativize(path);
    } else if (project.getRoot().resolve(path).toFile().exists()) {
      this.projPath = project.getRoot().resolve(path);
    } else {
      throw new IllegalArgumentException("could not find " + path + " in " + project.getRoot());
    }
  }

  public ProjectFile(CorpusProject project, String path) {
    this(project, Paths.get(path));
  }

  public boolean exists() {
    return toFile().exists();
  }

  public File toFile() {
    return project.getRoot().resolve(projPath).toFile();
  }

  public Path getProjectPath() {
    return projPath;
  }

  @Override
  public String toString() {
    return toFile().toString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    ProjectFile that = (ProjectFile) o;
    return Objects.equals(projPath, that.projPath);
  }

  @Override
  public int hashCode() {
    return Objects.hash(projPath);
  }
}
