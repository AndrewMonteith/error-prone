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

package com.google.errorprone.bugtrack.harness;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class TempFileModifier extends java.io.File implements AutoCloseable {
  private final Path originalFile;
  private final File tempFile;

  public TempFileModifier(Path originalFile) throws IOException {
    super(originalFile.toString());

    this.originalFile = originalFile;
    this.tempFile = new File(originalFile.toAbsolutePath() + ".temp");

    Files.copy(originalFile, tempFile.toPath());
  }

  @Override
  public void close() throws Exception {
    Files.delete(originalFile);
    tempFile.renameTo(originalFile.toFile());
  }
}
