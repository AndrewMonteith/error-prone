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

import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;

public final class DiagnosticUtils {
    public static final String CORPUS_ROOT = "/home/monty/IdeaProjects/java-corpus";

    private static int ordinalIndexOf(String text, char needle, int n) {
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == needle) {
                n--;
                if (n == 0) {
                    return i;
                }
            }
        }
        return -1;
    }

    public static String getProjectRelativePath(Diagnostic<? extends JavaFileObject> diagnostic) {
        String absolutePath = diagnostic.getSource().getName();

        if (absolutePath.startsWith(CORPUS_ROOT)) {
            return absolutePath.substring(ordinalIndexOf(absolutePath, '/', 6) + 1);
        } else {
            return absolutePath;
        }
    }

    public static String extractDiagnosticType(Diagnostic<? extends JavaFileObject> diagnostic) {
        String message = diagnostic.getMessage(null);
        return message.substring(1, message.indexOf(']'));
    }
}
