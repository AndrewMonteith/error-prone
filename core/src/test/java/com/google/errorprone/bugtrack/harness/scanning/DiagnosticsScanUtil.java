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

import com.google.common.collect.Lists;
import com.google.errorprone.bugtrack.projects.CorpusProject;
import com.google.errorprone.bugtrack.projects.ProjectFile;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class DiagnosticsScanUtil {
    public static void save(Collection<DiagnosticsScan> scan, String output) throws IOException {
        save(scan, Paths.get(output));
    }

    public static Collection<DiagnosticsScan> chunkScan(DiagnosticsScan scan, int maxFiles) {
        Collection<DiagnosticsScan> scans = new ArrayList<>();

        Lists.partition(scan.files, maxFiles).forEach(chunkOfFiles ->
                scans.add(new DiagnosticsScan(scan.name, chunkOfFiles, scan.cmdLineArguments)));

        return scans;
    }

    public static void save(Collection<DiagnosticsScan> scans, Path path) throws IOException {
        try (FileWriter output = new FileWriter(path.toFile())) {
            output.write(String.valueOf(scans.size()));
            output.write('\n');

            for (DiagnosticsScan scan : scans) {
                output.write("Name:\n");
                output.write(scan.name);
                output.write('\n');

                output.write("Files:\n");
                for (ProjectFile projFile : scan.files) {
                    output.write(projFile.getProjectPath().toString());
                    output.write('\n');
                }

                output.write("Args:\n");
                for (String cmdLineArg : scan.cmdLineArguments) {
                    output.write(cmdLineArg);
                    output.write('\n');
                }

                output.write("---\n");
            }
        }
    }

    public static Collection<DiagnosticsScan> load(CorpusProject project, Path path) throws IOException {
        List<DiagnosticsScan> scans = new ArrayList<>();
        List<String> lines = Files.readAllLines(path);

        final int numberOfScans = Integer.parseInt(lines.get(0));
        for (int i = 1; i < lines.size(); ) {
            String name = lines.get(++i); // Skip Name: and read

            i += 2; // name and Files:

            List<ProjectFile> files = new ArrayList<>();
            while (!lines.get(i).equals("Args:")) {
                files.add(new ProjectFile(project, lines.get(i)));
                ++i;
            }

            ++i; // Name:

            List<String> args = new ArrayList<>();
            while (!lines.get(i).equals("---")) {
                args.add(lines.get(i));
                ++i;
            }

            ++i; // ---

            scans.add(new DiagnosticsScan(name, files, args));
        }

        if (scans.size() != numberOfScans) {
            throw new RuntimeException("failed to read " + path.toString());
        }

        return scans;
    }

    public static Collection<DiagnosticsScan> load(CorpusProject project, String file) throws IOException {
        return load(project, Paths.get(file));
    }

}
