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

import java.util.Objects;

public class DiagnosticPositionRange {
    public final DiagnosticPosition startPos;
    public final DiagnosticPosition endPos;

    public DiagnosticPositionRange(final DiagnosticPosition startPos, final DiagnosticPosition endPos) {
        this.startPos = startPos;
        this.endPos = endPos;
    }

    public DiagnosticPositionRange(final long startLine, final long startCol, final long endLine, final long endCol) {
        this(new DiagnosticPosition(startLine, startCol), new DiagnosticPosition(endLine, endCol));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DiagnosticPositionRange that = (DiagnosticPositionRange) o;
        return startPos.equals(that.startPos) && endPos.equals(that.endPos);
    }

    @Override
    public int hashCode() {
        return Objects.hash(startPos, endPos);
    }

    @Override
    public String toString() {
        return "[" + startPos + ", " + endPos + "]";
    }
}
