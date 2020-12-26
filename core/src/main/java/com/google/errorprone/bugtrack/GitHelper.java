/*
 * Copyright 2020 The Error Prone Authors.
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

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ObjectId;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

public class GitHelper {
    private Git git;

    public GitHelper(String rootDir) throws java.io.IOException {
        Path path = Paths.get(rootDir);
        if (!Files.isDirectory(path)) {
            throw new IllegalArgumentException(rootDir + " must be a directory.");
        }

        this.git = Git.open(path.toFile());
    }

    public boolean isCommit(ObjectId commit) throws GitAPIException {
        return Stream.of(git.log().call()).anyMatch(logCommit -> logCommit == commit);
    }
}
