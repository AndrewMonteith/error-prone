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

package com.google.errorprone.bugtrack.motion;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.errorprone.bugtrack.DatasetDiagnostic;
import com.sun.tools.javac.util.Position;

import java.util.Arrays;
import java.util.List;

public class SrcFile {
    private final String name;
    private final ImmutableList<String> src;

    private final char[] charBuf;
    private final Position.LineMap lineMap;

    private static String expandTabs(String line) {
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

    public SrcFile(String fileName, List<String> src) {
        this.name = fileName;
        this.src = ImmutableList.copyOf(Iterables.transform(src, SrcFile::expandTabs));
        this.charBuf = Joiner.on('\n').join(this.src).toCharArray();
        this.lineMap = Position.makeLineMap(charBuf, charBuf.length, true);
    }

    public String getName() {
        return name;
    }

    public ImmutableList<String> getLines() {
        return src;
    }

    public String getSrc() {
        return Joiner.on('\n').join(src);
    }

    public long getLineNumber(final long pos) {
        return lineMap.getLineNumber(pos);
    }

    public long getColumnNumber(final long pos) {
        return lineMap.getColumnNumber(pos);
    }

    public long getPosition(final long line, final long col) {
        return lineMap.getPosition(line, col);
    }

    // start=1 will get first character
    public String getSrcExtract(final int start, final int end) {
        String code = String.valueOf(Arrays.copyOfRange(charBuf, start, end));
        if (code.endsWith(";")) { // normalize since some source ranges are [], some [).
            code = code.substring(0, code.length() - 1);
        }

        return code;
    }

    public String getSrcExtract(DatasetDiagnostic diagnostic) {
        return getSrcExtract((int) diagnostic.getStartPos(), (int) diagnostic.getEndPos());
    }
}
