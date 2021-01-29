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

import com.google.common.base.Joiner;
import com.google.errorprone.bugtrack.projects.ProjectFile;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DiagnosticsScan {
    public final String name;
    public final List<ProjectFile> files;
    public final List<String> cmdLineArguments;

    public DiagnosticsScan(String name, List<ProjectFile> files, List<String> cmdLineArguments) {
        this.name = name;
        this.files = files;
        this.cmdLineArguments = cmdLineArguments;
    }

    public DiagnosticsScan(DiagnosticsScan scan) {
        this.name = scan.name;
        this.files = new ArrayList<>(scan.files);
        this.cmdLineArguments = new ArrayList<>(scan.cmdLineArguments);
    }

    @Override
    public String toString() {
        return "Name " + name + "\n" +
                "Files:\n  " + Joiner.on("  \n").join(files) + "\n" +
                "Args: " + Joiner.on(' ').join(cmdLineArguments) + "\n";
    }
}
