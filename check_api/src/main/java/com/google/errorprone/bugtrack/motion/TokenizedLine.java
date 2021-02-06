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

import com.google.common.collect.ImmutableList;
import com.google.errorprone.util.ErrorProneToken;
import com.google.errorprone.util.ErrorProneTokens;
import com.sun.tools.javac.util.Context;

import java.util.Arrays;

public final class TokenizedLine {
    private final ImmutableList<ErrorProneToken> tokens;
    private final String srcLine;
    private final int hash;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TokenizedLine that = (TokenizedLine) o;
        return hash == that.hash;
    }

    @Override
    public int hashCode() {
        return hash;
    }

    private String readToken(ErrorProneToken token) {
        if (token.pos() == token.endPos()) {
            return "";
        }

        return srcLine.substring(token.pos(), token.endPos());
    }

    public TokenizedLine(String srcLine, Context context) {
        this.srcLine = srcLine;
        this.tokens = ErrorProneTokens.getTokens(srcLine, context);
        this.hash = Arrays.hashCode(tokens.stream().map(this::readToken).toArray());
    }
}
