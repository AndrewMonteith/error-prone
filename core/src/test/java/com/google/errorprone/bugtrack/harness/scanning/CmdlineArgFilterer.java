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

package com.google.errorprone.bugtrack.harness.scanning;

import com.google.common.collect.ImmutableSet;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static com.google.errorprone.bugtrack.projects.ShouldScanUtils.isJavaFile;

public final class CmdlineArgFilterer {
  private static final Set<String> ARGUMENT_BLOCKLIST =
      ImmutableSet.of("-nowarn", "-deprecation", "-verbose", "-deprecation");

  private static final Set<String> UNSUPPORTED_EARLY_JAVA_VERSIONS =
      ImmutableSet.of("1.5", "1.6", "1.7", "5", "6", "7");

  private CmdlineArgFilterer() {}

  /** Filters specified 'args' for all flags needed to compilation * */
  public static List<String> filter(List<String> args) {
    List<String> result = new ArrayList<>();

    for (int i = 0; i < args.size(); ++i) {
      if (ARGUMENT_BLOCKLIST.contains(args.get(i))) {
        continue;
      } else if (args.get(i).equals("-d")) {
        ++i;
        continue;
      } else if (args.get(i).startsWith("(") || args.get(i).startsWith("[")) {
        continue;
      } else if (isJavaFile(args.get(i))) {
        continue;
      } else if (args.get(i).equals("-target") || args.get(i).equals("-source")) {
        if (UNSUPPORTED_EARLY_JAVA_VERSIONS.contains(args.get(i + 1))) {
          result.set(i + 1, "1.8");
        }
      } else if (args.get(i).startsWith("-Xlint")) {
        continue;
      } else if (args.get(i).startsWith("-Xdoclint")) {
        continue;
      } else if (args.get(i).startsWith("-W")) {
        continue;
      }

      result.add(args.get(i));
    }

    return result;
  }
}
