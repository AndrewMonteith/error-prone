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

package com.google.errorprone.bugtrack.motion.trackers;

import com.github.difflib.algorithm.DiffException;
import com.google.errorprone.bugtrack.DatasetDiagnostic;
import com.google.errorprone.bugtrack.motion.DiagPosEqualityOracle;
import com.google.errorprone.util.ErrorProneToken;

import java.util.List;
import java.util.Optional;

public class TokenizedLineTracker implements DiagnosticPositionTracker {
    private final SrcLineTracker<TokenizedLine> srcTracker;

    public TokenizedLineTracker(List<TokenizedLine> oldTokens,
                                List<TokenizedLine> newTokens) throws DiffException {
        this.srcTracker = new SrcLineTracker<>(oldTokens, newTokens);
    }

    private long getNewColumn(final long oldLineNum, final long oldColumn, final long newLineNum) {
        TokenizedLine oldLine = srcTracker.getOldLine(oldLineNum);
        TokenizedLine newLine = srcTracker.getNewLine(newLineNum);

        int oldTokenIndex = oldLine.getTokenIndexForColumn(oldColumn);
        ErrorProneToken oldToken = oldLine.getToken(oldTokenIndex);
        ErrorProneToken newToken = newLine.getToken(oldTokenIndex);

        return newToken.pos() + (oldColumn-oldToken.pos());
    }

    @Override
    public Optional<DiagPosEqualityOracle> track(DatasetDiagnostic diag) {
        final long lineNum = diag.getLineNumber();
        final long col = diag.getColumnNumber();

        return srcTracker.getNewLineNumber(lineNum).map(newLineNum ->
                DiagPosEqualityOracle.byLineCol(newLineNum, getNewColumn(lineNum, col, newLineNum)));
    }
}
