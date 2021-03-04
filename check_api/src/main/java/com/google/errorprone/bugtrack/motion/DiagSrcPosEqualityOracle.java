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

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * When tracking diagnostics, we might want to track them via their line and column number or by it's start
 * position. Sadly the line/col information of a diagnostic is not guarenteed to map to a specific start/end buffer
 * location which means when trying to pipeline comparers we might have some difficulty. To simplify this matter this
 * class acts as a discriminated union for "position" information for a diagnostic. An owner of an instance need not
 * know whether it's tracking line/col or src buffer but can merely query whether a diagnostic matches it's location.
 */
public class DiagSrcPosEqualityOracle implements DiagPosEqualityOracle {
    private final EqualityType eqType;
    private final long line;
    private final long col;
    private final long startPos;
    private final long pos;
    private final long endPos;

    private DiagSrcPosEqualityOracle(EqualityType eqType, long line, long col, long startPos, long pos, long endPos) {
        this.eqType = eqType;
        this.line = line;
        this.col = col;
        this.startPos = startPos;
        this.pos = pos;
        this.endPos = endPos;
    }

    public static DiagSrcPosEqualityOracle byStartPos(final long startPos) {
        return new DiagSrcPosEqualityOracle(EqualityType.BY_START_POS, -1, -1, startPos, -1, -1);
    }

    public static DiagSrcPosEqualityOracle byStartAndEndPos(final long startPos, final long endPos) {
        return new DiagSrcPosEqualityOracle(EqualityType.BY_START_AND_END_POS, -1, -1, startPos, -1, endPos);
    }

    public static DiagSrcPosEqualityOracle byLineCol(final long line, final long col) {
        return new DiagSrcPosEqualityOracle(EqualityType.BY_LINE, line, col, -1, -1, -1);
    }

    public static DiagPosEqualityOracle byPosition(long pos) {
        return new DiagSrcPosEqualityOracle(EqualityType.BY_POS, -1, -1, -1, pos, -1);
    }

    public long getLine() {
        if (eqType != EqualityType.BY_LINE) {
            throw new RuntimeException("cannot get line if not tracking by line");
        }

        return line;
    }

    public long getCol() {
        if (eqType != EqualityType.BY_LINE) {
            throw new RuntimeException("cannot get col if not tracking by line");
        }

        return col;
    }

    public long getStartPos() {
        if (!(eqType == EqualityType.BY_START_POS || eqType == EqualityType.BY_START_AND_END_POS)) {
            throw new RuntimeException("cannot get start position if not tracking by start, and possibly end, pos");
        }

        return startPos;
    }

    public long getEndPos() {
        if (eqType != EqualityType.BY_START_AND_END_POS) {
            throw new RuntimeException("cannot get end pos if not tracking by start and end pos");
        }

        return endPos;
    }

    public long getPos() {
        if (eqType != EqualityType.BY_POS) {
            throw new RuntimeException("cannot get pos if not tracking by pos");
        }

        return pos;
    }

    @Override
    public boolean hasSamePosition(DatasetDiagnostic diag) {
        switch (eqType) {
            case BY_LINE:
                return diag.getLineNumber() == this.line && diag.getColumnNumber() == this.col;
            case BY_START_POS:
                return diag.getStartPos() == startPos;
            case BY_START_AND_END_POS:
                return diag.getStartPos() == startPos && diag.getEndPos() == endPos;
            case BY_POS:
                return diag.getPos() == pos;
            default:
                throw new RuntimeException("unsupported equality type");
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DiagSrcPosEqualityOracle that = (DiagSrcPosEqualityOracle) o;
        if (eqType != that.eqType) {
            return false;
        }

        switch (eqType) {
            case BY_LINE:
                return line == that.line && col == that.col;
            case BY_START_POS:
                return startPos == that.startPos;
            case BY_START_AND_END_POS:
                return startPos == that.startPos && endPos == that.endPos;
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
            case BY_POS:
                return "[" + eqType + ", " + pos + "]";
            case BY_START_AND_END_POS:
                return "[" + eqType + ", [" + startPos + "," + endPos + "]]";
            default:
                throw new RuntimeException("unexpected equality type");
        }
    }

    enum EqualityType {
        BY_LINE,
        BY_START_POS,
        BY_START_AND_END_POS,
        BY_POS
    }
}
