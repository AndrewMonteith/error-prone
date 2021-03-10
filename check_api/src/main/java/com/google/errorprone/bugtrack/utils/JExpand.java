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

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

/*
    Before we scan a project we run the unix expand utility on all of them
    Turns out this is a bit smarter than I had anticipated and doesnt't just replace trailing '\t' with "    "
    But when reading a file from a git version we ideally want the same processing performed
    Sadly one cannot merely invoke expand with a string nor do I want to write files to a disk
      to run expand on them since HPC disk IO is woefully slow.
    So since non-trivial mismatch cases don't occur too often this class will approximate expand as necessary
      when corner cases are encountered
 */
public class JExpand {
    private static boolean containsMixedIndentation(String line) {
        boolean foundTab = false;
        boolean foundSpace = false;

        for (int i = 0; i < line.length(); ++i) {
            char c = line.charAt(i);
            if (c == ' ') {
                foundSpace = true;
            } else if (c == '\t') {
                foundTab = true;
            } else {
                break;
            }
        }

        return foundTab && foundSpace;
    }

    private static int countPreceedingSpaces(String line) {
        int spaces = 0;
        for (; spaces < line.length() && line.charAt(spaces) == ' '; ++spaces);
        return spaces;
    }

    private static String fixMixedIndentation(String prevExpandedLine, String expandedLine) {
        int indentAbove = countPreceedingSpaces(prevExpandedLine);
        int indent = countPreceedingSpaces(expandedLine);

        if (indentAbove == 0 || indent == indentAbove || indent == indentAbove + 4) {
            return expandedLine;
        } else {
            int newIndent = indentAbove + (prevExpandedLine.endsWith("{") ? 4 : 0);
            return StringUtils.repeat(" ", newIndent) + StringUtils.stripStart(expandedLine, null);
        }
    }

    private static String expandPreceedingTabs(String line) {
        if (line.isEmpty()) {
            return "";
        }

        StringBuilder result = new StringBuilder();

        int i = 0;
        while (i < line.length() && (line.charAt(i) == '\t' || line.charAt(i) == ' ')) {
            char c = line.charAt(i);
            result.append(c == '\t' ? "    " : " ");
            ++i;
        }

        result.append(line.substring(i));

        return result.toString();
    }

    public static List<String> expand(List<String> lines) {
        List<String> result = new ArrayList<>();

        for (int i = 0; i < lines.size(); ++i) {
            String line = lines.get(i);
            String expanded = expandPreceedingTabs(line);

            if (containsMixedIndentation(line)) {
                expanded = fixMixedIndentation(i > 0 ? result.get(i-1) : "", expanded);
            }

            result.add(expanded);
        }

        return result;
    }

}
