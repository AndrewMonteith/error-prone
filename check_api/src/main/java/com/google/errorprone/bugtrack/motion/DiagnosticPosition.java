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

import com.google.errorprone.bugtrack.DatasetDiagnostic;

import java.util.Objects;

public final class DiagnosticPosition {
    public final long line;
    public final long column;

    public DiagnosticPosition(final long line, final long column) {
        if (line < 0 || column < 0) {
            // Java has no inbuilt unsigned integer?
            throw new IllegalArgumentException("cannot have negative positions");
        }
        this.line = line;
        this.column = column;
    }

    public DiagnosticPosition(DatasetDiagnostic diagnostic) {
        this(diagnostic.getLineNumber(), diagnostic.getColumnNumber());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DiagnosticPosition that = (DiagnosticPosition) o;
        return line == that.line && column == that.column;
    }

    @Override
    public int hashCode() {
        return Objects.hash(line, column);
    }

    @Override
    public String toString() {
        return "(" + line + ", " + column + ")";
    }
}
