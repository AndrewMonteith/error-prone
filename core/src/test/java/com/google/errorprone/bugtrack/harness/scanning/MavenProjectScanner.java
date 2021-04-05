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

import com.google.common.base.Splitter;
import com.google.errorprone.bugtrack.projects.CorpusProject;
import com.google.errorprone.bugtrack.utils.ProjectFiles;
import com.google.errorprone.bugtrack.utils.ShellUtils;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class MavenProjectScanner extends ProjectScanner {
  @Override
  public void cleanProject(Path projectDir) throws IOException, InterruptedException {
    //        ShellUtils.runCommand(projectDir, "mvn", "clean");
    ShellUtils.runCommand(
        projectDir, "/bin/bash", ProjectFiles.find("error-prone", "clean_proj.sh").toString());
  }

  public Collection<DiagnosticsScan> parseScansOutput(CorpusProject project, String buildOutput) {
    CmdBlobFilesExtractor filesExtractor = project.getFilesExtractor();

    Collection<DiagnosticsScan> scans = new ArrayList<>();

    List<String> buildOutputLines = Splitter.on('\n').splitToList(buildOutput.trim());
    for (int i = 0; i < buildOutputLines.size(); i += 2) {
      String scanName = buildOutputLines.get(i);
      List<String> cmdlineBlob = Splitter.on(' ').splitToList(buildOutputLines.get(i + 1));
      scans.add(
          new DiagnosticsScan(
              scanName,
              /* files= */ filesExtractor.extract(cmdlineBlob),
              /* cmdlineArgs= */ CmdlineArgFilterer.filter(cmdlineBlob)));
    }

    return scans;
  }

  @Override
  public Collection<DiagnosticsScan> getScans(CorpusProject project)
      throws IOException, InterruptedException {
    System.out.println("Building");
    String buildOutput =
        ShellUtils.runCommand(
            project.getRoot(),
            "/usr/bin/python3",
            ProjectFiles.find("error-prone", "get_maven_cmdargs.py").toString(),
            project.getRoot().toString());

    System.out.println("parsing output");
    return parseScansOutput(project, buildOutput);
  }
}
