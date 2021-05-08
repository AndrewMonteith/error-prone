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

import java.util.Optional;

class DiagnosticPositionChanger {
    private final DatasetDiagnostic originalDiagnostic;
    private Optional<Long> startPos = Optional.empty();
    private Optional<Long> pos = Optional.empty();
    private Optional<Long> endPos = Optional.empty();

    DiagnosticPositionChanger(DatasetDiagnostic diagnostic) {
        this.originalDiagnostic = diagnostic;
    }

    static DiagnosticPositionChanger on(DatasetDiagnostic diagnostic) {
        return new DiagnosticPositionChanger(diagnostic);
    }

    public DatasetDiagnostic build() {
        return new DatasetDiagnostic(
                originalDiagnostic.getFileName(),
                originalDiagnostic.getLineNumber(),
                originalDiagnostic.getColumnNumber(),
                startPos.orElse(originalDiagnostic.getStartPos()),
                pos.orElse(originalDiagnostic.getPos()),
                endPos.orElse(originalDiagnostic.getEndPos()),
                originalDiagnostic.getMessage());
    }

    DiagnosticPositionChanger setStartPos(final long pos) {
        this.startPos = Optional.of(pos);
        return this;
    }

    DiagnosticPositionChanger setPos(final long pos) {
        this.pos = Optional.of(pos);
        return this;
    }

    DiagnosticPositionChanger setEndPos(final long pos) {
        this.endPos = Optional.of(pos);
        return this;
    }
}
