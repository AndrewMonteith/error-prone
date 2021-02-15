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
import com.google.errorprone.bugtrack.utils.Thunk;
import com.sun.tools.javac.util.Position;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class SrcFile {
    private final String name;
    private final ImmutableList<String> src;

    private final Thunk<char[]> getBufferThunk;
    private final Thunk<Position.LineMap> lineMapThunk;

    private int versionNum; // flag used in hashing to indicate old and new versions with the same name

    public String getName() {
        return name;
    }
    public void setVersionNum(int num) {
        this.versionNum = num;
    }
    public int getVersionNum() { return versionNum; }

    public ImmutableList<String> getLines() {
        return src;
    }

    public String getSrc() {
        return Joiner.on('\n').join(src);
    }

    public File getTempFileOnDisk() {
        Path testFilesDir = Paths.get("/home/monty/IdeaProjects/java-corpus/temp_files");
        Path testFile = testFilesDir.resolve(new File(name).getName() + "_" + versionNum + ".java");

        try {
            Files.write(testFile, new String(getBufferThunk.get()).getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            e.printStackTrace();
        }

        return testFile.toFile();
    }

    public long getLineNumber(final long pos) {
        return lineMapThunk.get().getLineNumber(pos);
    }

    public long getColumnNumber(final long pos) {
        return lineMapThunk.get().getColumnNumber(pos);
    }

    public long getPosition(final long line, final long col) {
        return lineMapThunk.get().getPosition(line, col);
    }

    public String getSrcExtract(final int start, final int end) {
        char[] buf = getBufferThunk.get();

        String code = String.valueOf(Arrays.copyOfRange(buf, start, end));
        if (code.endsWith(";")) { // normalize since some source ranges are [], some [).
            code = code.substring(0, code.length() - 1);
        }

        return code;
    }

    public String getSrcExtract(DatasetDiagnostic diagnostic) {
        return getSrcExtract((int) diagnostic.getStartPos(), (int) diagnostic.getEndPos());
    }

    private static String normalize(String line) {
        return line.replace("\t", "    ");
    }

    public SrcFile(String fileName, List<String> src) {
        this.name = fileName;
        this.src = ImmutableList.copyOf(Iterables.transform(src, SrcFile::normalize));
        this.versionNum = 0;

        this.getBufferThunk = new Thunk<>(() -> Joiner.on('\n').join(this.src).toCharArray());

        this.lineMapThunk = new Thunk<>(() -> {
            char[] buf = getBufferThunk.get();
            return Position.makeLineMap(buf, buf.length, true);
        });
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SrcFile srcFile = (SrcFile) o;
        return versionNum == srcFile.versionNum && name.equals(srcFile.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, versionNum);
    }
}
