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

import com.github.difflib.DiffUtils;
import com.github.difflib.algorithm.DiffException;
import com.github.difflib.algorithm.jgit.HistogramDiff;
import com.github.difflib.patch.AbstractDelta;
import com.github.difflib.patch.Chunk;
import com.github.difflib.patch.Patch;

import java.util.List;
import java.util.Optional;

/*
    Tracks source lines between two versions of the code in the same file.
    T is the datatype represents a single line of code, currently String or TokenizedLine is used.
 */
class SrcLineTracker<T> {
    private final List<T> oldSrc;
    private final List<T> newSrc;
    private final Patch<T> patch;

    public SrcLineTracker(List<T> oldSrc, List<T> newSrc) throws DiffException {
        this.oldSrc = oldSrc;
        this.newSrc = newSrc;
        this.patch = DiffUtils.diff(oldSrc, newSrc, new HistogramDiff<>());
    }

    private boolean isLineInChunk(Chunk<T> chunk, final long line) {
        return chunk.getPosition() <= line && line < (chunk.getPosition() + chunk.getLines().size());
    }

    private Optional<AbstractDelta<T>> findSourcePatchAffectingLine(final long line) {
        return patch.getDeltas().stream()
                .filter(delta -> isLineInChunk(delta.getSource(), line))
                .findFirst();
    }

    private int getShiftCausedByPatchesAboveLine(final long line) {
        return patch.getDeltas().stream()
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

    public Optional<Long> getNewLineNumber(final long line) {
        final long internalLine = line - 1;

        Optional<AbstractDelta<T>> patch = findSourcePatchAffectingLine(internalLine);

        if (patch.isPresent()) {
            Chunk<T> target = patch.get().getTarget();
            switch (patch.get().getType()) {
                case EQUAL:
                    throw new RuntimeException("unsure when i'd encounter this");
                case INSERT:
                    return Optional.of((long) target.getPosition() + target.size());
                case CHANGE:
                    int offsetInChange = target.getLines().indexOf(oldSrc.get((int) internalLine));
                    return offsetInChange == -1 ? Optional.empty() : Optional.of((long) target.getPosition() + offsetInChange);
                default: // case DELETE:
                    // If the line is deemed "deleted" from the old source file, then logically it's not in the other
                    // file. Sadly this can be misleading, see (Classes of source code transformations).
                    return Optional.empty();
            }
        } else {
            return Optional.of(line + getShiftCausedByPatchesAboveLine(internalLine));
        }
    }

    public T getOldLine(final long lineNumber) {
        return oldSrc.get((int) (lineNumber-1));
    }

    public T getNewLine(final long lineNumber) {
        return newSrc.get((int) (lineNumber-1));
    }
}
