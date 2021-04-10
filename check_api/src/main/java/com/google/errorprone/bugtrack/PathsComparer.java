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

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

@FunctionalInterface
public interface PathsComparer {
  Optional<Path> getNewPath(Path oldPath);

  default Optional<Path> getNewPath(String oldPath) {
    return getNewPath(Paths.get(oldPath));
  }

  default boolean isSameFile(Path path1, Path path2) {
    Optional<Path> newPath = getNewPath(path1);

    return newPath.isPresent() && newPath.get().equals(path2);
  }

  default boolean isSameFile(String path1, String path2) {
    return isSameFile(Paths.get(path1), Paths.get(path2));
  }

  default boolean isSameFile(DatasetDiagnostic oldDiagnostic, DatasetDiagnostic newDiagnostic) {
    return isSameFile(oldDiagnostic.getFileName(), newDiagnostic.getFileName());
  }
}
