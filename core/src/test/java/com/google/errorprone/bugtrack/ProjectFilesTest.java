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

import com.google.errorprone.bugtrack.utils.ProjectFiles;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.nio.file.Path;

public class ProjectFilesTest {

    @Test
    public void getsFilesThatExists() {
        // GIVEN:
        String fileName = "error-prone/pom.xml";

        // WHEN:
        Path f = ProjectFiles.get(fileName);

        // THEN:
        Assert.assertEquals("/home/monty/IdeaProjects/error-prone/pom.xml", f.toString());
    }

    @Test
    public void findsFileThatExists() {
        // GIVEN:
        String prefix = "error-prone";
        String fileName = "ProjectFilesTest.java";

        // WHEN:
        Path f = ProjectFiles.find(prefix, fileName);

        // THEN:
        Assert.assertEquals(
                "/home/monty/IdeaProjects/error-prone/core/src/test/java/com/google/errorprone/bugtrack/ProjectFilesTest.java",
                f.toString());
    }

    @Test
    public void errorsWhenFindingAmbigousFiles() {
        // GIVEN:
        String prefix = "error-prone";
        String fileName = "pom.xml";

        // WHEN & THEN:
        Assert.assertThrows(RuntimeException.class, () -> {
            ProjectFiles.find(prefix, fileName);
        });
    }

}
