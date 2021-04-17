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

package com.google.errorprone.bugtrack.harness.evaluating;

import com.google.errorprone.bugtrack.projects.CorpusProject;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

public final class LiveDatsetFilePairLoaderTest {
  private static final String testDiagnosticFiles =
      "src/test/java/com/google/errorprone/bugtrack/testdata/fake_dataset_files";
  private static final CorpusProject TEST_PROJECT =
      new CorpusProject() {
        @Override
        public Path getRoot() {
          return Paths.get("root/java-corpus/test");
        }

        @Override
        public boolean shouldScanFile(Path file) {
          return true;
        }

        @Override
        public BuildSystem getBuildSystem() {
          return BuildSystem.Maven;
        }
      };

  @Test
  public void canLoadAllFiles() {
    Assert.assertEquals(
        5, RandomDiagFilePairLoader.allFiles(TEST_PROJECT, testDiagnosticFiles).getNumberOfFiles());
  }

  @Test
  public void canLoadFilesRespectingRange() {
    // GIVEN:
    IntRanges range = IntRanges.include(1, 5).excludeRange(2, 3);

    // WHEN:
    RandomDiagFilePairLoader loader =
        RandomDiagFilePairLoader.inSeqNumRange(TEST_PROJECT, testDiagnosticFiles, range);

    // THEN:
    Assert.assertEquals(3, loader.getNumberOfFiles());
  }

  @Test
  public void pairsLoadedArentBackwards() throws IOException {
    // GIVEN:
    RandomDiagFilePairLoader loader = RandomDiagFilePairLoader.allFiles(TEST_PROJECT, testDiagnosticFiles);

    // WHEN:
    ArrayList<DiagnosticsFilePairLoader.Pair> pairs = new ArrayList<>();
    for (int i = 0; i < 1000; ++i) {
      pairs.add(loader.load());
    }

    // THEN:
    Assert.assertTrue(
        pairs.stream()
            .allMatch(pair -> pair.oldFile.commitId.compareTo(pair.newFile.commitId) <= 0));
  }
}
