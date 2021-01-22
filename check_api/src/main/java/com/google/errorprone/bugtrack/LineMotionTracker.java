/*
 * Copyright 2020 The Error Prone Authors.
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

package com.google.errorprone.bugtrack;

import com.github.difflib.DiffUtils;
import com.github.difflib.algorithm.DiffException;
import com.github.difflib.algorithm.jgit.HistogramDiff;
import com.github.difflib.patch.AbstractDelta;
import com.github.difflib.patch.Chunk;
import com.github.difflib.patch.Patch;
import com.google.common.collect.Iterables;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public final class LineMotionTracker {
    private final List<String> oldText;
    private final List<String> newText;

    private final Patch<String> filePatch;

    public LineMotionTracker(List<String> oldText, List<String> newText) throws DiffException {
        this.oldText = oldText;
        this.newText = newText;
        this.filePatch = DiffUtils.diff(oldText, newText, new HistogramDiff<>());
    }

    public LineMotionTracker(String oldText, String newText) throws DiffException {
        this(Arrays.asList(oldText.split("\n").clone()),
                Arrays.asList(newText.split("\n").clone()));
    }

    private static boolean isLineInChunk(Chunk<String> chunk, final long line) {
        return chunk.getPosition() <= line && line < (chunk.getPosition() + chunk.getLines().size());
    }

    private Optional<AbstractDelta<String>> findSourcePatchAffectingLine(final long line) {
        return filePatch.getDeltas().stream()
                .filter(delta -> isLineInChunk(delta.getSource(), line))
                .findFirst();
    }

    private int getShiftCausedByPatchesAboveLine(final long line) {
        return filePatch.getDeltas().stream()
                .filter(delta -> delta.getSource().getPosition() + delta.getSource().size() <= line)
                .mapToInt(delta -> {
                    switch (delta.getType()) {
                        case DELETE:
                            return -delta.getSource().size();
                        case INSERT:
                            return delta.getTarget().size();
                        case CHANGE:
                            return delta.getTarget().size() - delta.getSource().size();
                        default:
                            return 0;
                    }
                }).sum();
    }

    public Optional<Long> getNewLine(final long line) throws RuntimeException {
        final long internalLine = line - 1;

        Optional<AbstractDelta<String>> patch = findSourcePatchAffectingLine(internalLine);

        if (patch.isPresent()) {
            Chunk<String> target = patch.get().getTarget();
            switch (patch.get().getType()) {
                case EQUAL:
                    throw new RuntimeException("unsure when i'd encounter this");
                case INSERT:
                    return Optional.of((long) target.getPosition() + target.size());
                case CHANGE:
                    int offsetInChange = target.getLines().indexOf(oldText.get((int) internalLine));
                    return offsetInChange == -1 ? Optional.empty() : Optional.of((long) target.getPosition() + offsetInChange);
                default: // case DELETE:
                    return Optional.empty();
            }
        } else {
            return Optional.of(line + getShiftCausedByPatchesAboveLine(internalLine));
        }
    }
}
