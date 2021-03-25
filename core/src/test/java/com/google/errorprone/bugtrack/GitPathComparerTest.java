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

import com.google.errorprone.bugtrack.projects.CorpusProject;
import com.google.errorprone.bugtrack.projects.MavenTestRepo;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

public final class GitPathComparerTest {

    @Test
    public void identifiesFilesWithSameName() throws IOException, GitAPIException {
        // GIVEN:
        CorpusProject testProject = new MavenTestRepo();
        String oldCommit = "05fe468e3e0c74a96057919c9262b0c8ccac2b5a";
        String newCommit = "816b2a9e0b350a07e4bedf85c638acec57ed0dee";

        // WHEN:
        PathsComparer comparer = new GitPathComparer(testProject.loadRepo(), oldCommit, newCommit);

        // THEN:
        Assert.assertTrue(comparer.isSameFile("src/main/java/com/mycompany/app/Test3.java", "src/main/java/com/mycompany/app/Test3.java"));
    }

    @Test
    public void identifiesFilesThatAreRenamedWithRelativePath() throws IOException, GitAPIException {
        // GIVEN:
        CorpusProject testProject = new MavenTestRepo();
        String oldCommit = "05fe468e3e0c74a96057919c9262b0c8ccac2b5a";
        String newCommit = "816b2a9e0b350a07e4bedf85c638acec57ed0dee";

        // WHEN:
        PathsComparer comparer = new GitPathComparer(testProject.loadRepo(), oldCommit, newCommit);

        // THEN:
        Assert.assertTrue(comparer.isSameFile("src/main/java/com/mycompany/app/Test2.java", "src/main/java/com/mycompany/app/Test3.java"));
    }

    @Test
    public void identifiesFilesThatAreRenamedWithAbsolutePath() throws IOException, GitAPIException {
        // GIVEN:
        CorpusProject testProject = new MavenTestRepo();
        String oldCommit = "05fe468e3e0c74a96057919c9262b0c8ccac2b5a";
        String newCommit = "816b2a9e0b350a07e4bedf85c638acec57ed0dee";

        // WHEN:
        PathsComparer comparer = new GitPathComparer(testProject.loadRepo(), oldCommit, newCommit);

        // THEN:
        Assert.assertTrue(comparer.isSameFile(
                "/home/monty/IdeaProjects/error-prone/core/src/test/java/com/google/errorprone/bugtrack/testdata/maven_test_repo/src/main/java/com/mycompany/app/Test2.java",
                "/home/monty/IdeaProjects/error-prone/core/src/test/java/com/google/errorprone/bugtrack/testdata/maven_test_repo/src/main/java/com/mycompany/app/Test3.java"));
    }
}
