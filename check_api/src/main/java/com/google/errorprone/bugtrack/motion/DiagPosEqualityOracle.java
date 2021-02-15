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

/**
 * When tracking diagnostics, we might want to track them via their line and column number or by it's start
 * position. Sadly the line/col information of a diagnostic is not guarenteed to map to a specific start/end buffer
 * location which means when trying to pipeline comparers we might have some difficulty. To simplify this matter this
 * class acts as a discriminated union for "position" information for a diagnostic. An owner of an instance need not
 * know whether it's tracking line/col or src buffer but can merely query whether a diagnostic matches it's location.
 */
public class DiagPosEqualityOracle {
    private final EqualityType eqType;
    private long line;
    private long col;
    private long startPos;

    public DiagPosEqualityOracle(EqualityType eqType, DatasetDiagnostic diagnostic) {
        this.eqType = eqType;

        if (eqType == EqualityType.BY_LINE) {
            this.line = diagnostic.getLineNumber();
            this.col = diagnostic.getColumnNumber();
        } else {
            this.startPos = diagnostic.getStartPos();
        }
    }

    private DiagPosEqualityOracle(final long startPos) {
        this.eqType = EqualityType.BY_START_POS;
        this.startPos = startPos;
    }

    private DiagPosEqualityOracle(final long line, final long col) {
        this.eqType = EqualityType.BY_LINE;
        this.line = line;
        this.col = col;
    }

    public static DiagPosEqualityOracle byStartPos(final long startPos) {
        return new DiagPosEqualityOracle(startPos);
    }

    public static DiagPosEqualityOracle byLineCol(final long line, final long col) {
        return new DiagPosEqualityOracle(line, col);
    }

    public long getLine() {
        if (eqType != EqualityType.BY_LINE) {
            throw new RuntimeException("cannot get line if not tracking by line");
        }

        return line;
    }

    public long getCol() {
        if (eqType != EqualityType.BY_LINE) {
            throw new RuntimeException("cannot get line if not tracking by line");
        }

        return col;
    }

    public long getStartPos() {
        if (eqType != EqualityType.BY_START_POS) {
            throw new RuntimeException("cannot get line if not tracking by line");
        }

        return startPos;
    }

    public boolean hasSamePosition(DatasetDiagnostic diag) {
        switch (eqType) {
            case BY_LINE:
                return diag.getLineNumber() == line && diag.getColumnNumber() == col;
            case BY_START_POS:
                return diag.getStartPos() == startPos;
            default:
                throw new RuntimeException("unsupported equality type");
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DiagPosEqualityOracle that = (DiagPosEqualityOracle) o;
        if (eqType != that.eqType) {
            return false;
        }

        switch (eqType) {
            case BY_LINE:
                return line == that.line && col == that.col;
            case BY_START_POS:
                return startPos == that.startPos;
            default:
                throw new RuntimeException("unexpected equality type");
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(eqType, line, col, startPos);
    }

    @Override
    public String toString() {
        switch (eqType) {
            case BY_LINE:
                return "[" + eqType + ", (" + line + "," + col + ")]";
            case BY_START_POS:
                return "[" + eqType + ", " + startPos + "]";
            default:
                throw new RuntimeException("unexpected equality type");
        }
    }

    enum EqualityType {
        BY_LINE,
        BY_START_POS
    }
}
