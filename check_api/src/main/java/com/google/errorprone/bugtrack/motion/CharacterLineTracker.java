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

import com.github.difflib.algorithm.DiffException;

import java.util.List;
import java.util.Optional;

/*

 */
public class CharacterLineTracker implements DiagnosticPositionTracker {
    private SrcLineTracker<String> srcTracker;

    public CharacterLineTracker(List<String> oldSrc, List<String> newSrc) throws DiffException {
        this.srcTracker = new SrcLineTracker<>(oldSrc, newSrc);
    }

    @Override
    public Optional<DiagnosticPosition> getNewPosition(long line, long column) {
        return srcTracker.getNewLineNumber(line).map(lineNumber -> new DiagnosticPosition(lineNumber, column));
    }

//    protected final List<T> oldSrc;
//    protected final List<T> newSrc;
//    protected final Patch<T> filePatch;
//
//    protected LineMotionTracker(List<T> oldSrc, List<T> newSrc, Patch<T> filePatch) {
//        this.oldSrc = oldSrc;
//        this.newSrc = newSrc;
//        this.filePatch = filePatch;
//    }
//
//    public static LineMotionTracker<String> newLineCharsTracker(List<String> oldSrc,
//                                                                List<String> newSrc) throws DiffException {
//        return new LineMotionTracker<>(
//                oldSrc, newSrc, DiffUtils.diff(oldSrc, newSrc, new HistogramDiff<>()));
//    }
//
//    public static LineMotionTracker<TokenizedLine> newLineTokenTracker(List<String> oldSrc,
//                                                                       Context oldSrcContext,
//                                                                       List<String> newSrc,
//                                                                       Context newSrcContext) throws DiffException {
//        List<TokenizedLine> oldSrcTokenized = oldSrc.stream().map(line -> new TokenizedLine(line, oldSrcContext)).collect(Collectors.toList());
//        List<TokenizedLine> newSrcTokenized = newSrc.stream().map(line -> new TokenizedLine(line, newSrcContext)).collect(Collectors.toList());
//
//        Patch<TokenizedLine> tokenizedLines = DiffUtils.diff(oldSrcTokenized, newSrcTokenized, new HistogramDiff<>());
//
//        return new LineColumnMotionTracker(oldSrcTokenized, newSrcTokenized, tokenizedLines);
//    }
//
//    private boolean isLineInChunk(Chunk<T> chunk, final long line) {
//        return chunk.getPosition() <= line && line < (chunk.getPosition() + chunk.getLines().size());
//    }
//
//    private Optional<AbstractDelta<T>> findSourcePatchAffectingLine(final long line) {
//        return filePatch.getDeltas().stream()
//                .filter(delta -> isLineInChunk(delta.getSource(), line))
//                .findFirst();
//    }
//
//    private int getShiftCausedByPatchesAboveLine(final long line) {
//        return filePatch.getDeltas().stream()
//                .filter(delta -> delta.getSource().getPosition() + delta.getSource().size() <= line)
//                .mapToInt(delta -> {
//                    switch (delta.getType()) {
//                        case DELETE:
//                            return -delta.getSource().size();
//                        case INSERT:
//                            return delta.getTarget().size();
//                        case CHANGE:
//                            return delta.getTarget().size() - delta.getSource().size();
//                        default:
//                            return 0;
//                    }
//                }).sum();
//    }
//
//    public Optional<DiagnosticPosition> getNewPosition(final long line, final long column) {
//        return getNewLine(line).map(newLineNumber ->
//                new DiagnosticPosition(newLineNumber, getNewColumn(line, newLineNumber, column)));
//    }
//
//    protected long getNewColumn(final long oldLine, final long newLine, final long oldColumn) {
//        return oldColumn;
//    }
//
//    private Optional<Long> getNewLine(final long line) throws RuntimeException {
//        final long internalLine = line - 1;
//
//        Optional<AbstractDelta<T>> patch = findSourcePatchAffectingLine(internalLine);
//
//        if (patch.isPresent()) {
//            Chunk<T> target = patch.get().getTarget();
//            switch (patch.get().getType()) {
//                case EQUAL:
//                    throw new RuntimeException("unsure when i'd encounter this");
//                case INSERT:
//                    return Optional.of((long) target.getPosition() + target.size());
//                case CHANGE:
//                    int offsetInChange = target.getLines().indexOf(oldSrc.get((int) internalLine));
//                    return offsetInChange == -1 ? Optional.empty() : Optional.of((long) target.getPosition() + offsetInChange);
//                default: // case DELETE:
//                    // If the line is deemed "deleted" from the old source file, then logically it's not in the other
//                    // file. Sadly this can be misleading, see (Classes of source code transformations).
//                    return Optional.empty();
//            }
//        } else {
//            return Optional.of(line + getShiftCausedByPatchesAboveLine(internalLine));
//        }
//    }
}
