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

import com.google.common.collect.Iterables;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.NameFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;

public class ProjectFiles {
    private static final String projRoot;
    
    static {
        String root = System.getenv("ROOT");
        if (root == null || root.isEmpty()) {
            projRoot = "/home/monty/IdeaProjects/";
        } else {
            projRoot = root;
        }
    }

    public static Path get(String path) {
        return Paths.get(projRoot, path);
    }

    public static Path find(String prefix, String fileName) {
        Collection<File> files = FileUtils.listFiles(Paths.get(projRoot, prefix).toFile(),
                new NameFileFilter(fileName),
                TrueFileFilter.TRUE);

        if (files.size() != 1) {
            throw new RuntimeException("non-unique file requested");
        }

        return Iterables.getLast(files).toPath();
    }
}
