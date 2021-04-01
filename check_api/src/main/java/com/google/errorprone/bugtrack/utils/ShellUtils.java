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

package com.google.errorprone.bugtrack.utils;

import com.google.common.base.Joiner;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

public final class ShellUtils {

  public static String runCommand(Path directory, String... command)
      throws IOException, InterruptedException {
    return runCommand(directory.toFile(), command);
  }

  public static String runCommand(File directory, String... command)
      throws IOException, InterruptedException {
    Process process = new ProcessBuilder().command(command).directory(directory).start();

    BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
    StringBuilder output = new StringBuilder();

    do {
      process.waitFor(1000, TimeUnit.MILLISECONDS);

      // Drain input to stop process buffer filling up and waitFor() hanging
      int b;
      while ((b = reader.read()) != -1) {
        output.append((char) b);
      }
    } while (process.isAlive());

    if (process.exitValue() != 0) {
      String errorMsg = "Failed to run command " + Joiner.on(' ').join(command) + "\n";
      errorMsg += new InputStreamReader(process.getErrorStream()).toString();
      throw new IOException(errorMsg);
    }

    return output.toString();
  }
}
