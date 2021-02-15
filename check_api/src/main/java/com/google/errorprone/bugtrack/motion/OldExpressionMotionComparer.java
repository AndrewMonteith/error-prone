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

//import ch.uzh.ifi.seal.changedistiller.ChangeDistiller;
//import ch.uzh.ifi.seal.changedistiller.distilling.FileDistiller;
//import ch.uzh.ifi.seal.changedistiller.model.classifiers.SourceRange;
//import ch.uzh.ifi.seal.changedistiller.model.entities.SourceCodeChange;
//import ch.uzh.ifi.seal.changedistiller.model.entities.Update;
//import com.google.errorprone.bugtrack.BugComparer;
//import com.google.errorprone.bugtrack.DatasetDiagnostic;
//import com.google.errorprone.bugtrack.utils.MemoMap;
//
//import java.io.File;
//import java.io.IOException;
//import java.util.Collection;

public class OldExpressionMotionComparer {
//    private final DiagnosticsDeltaManager diagnosticsDeltaManager;
//    private final MemoMap<SrcFile, Collection<SourceCodeChange>> exprTrackers;
//
//    public OldExpressionMotionComparer(DiagnosticsDeltaManager diagnosticsDeltaManager) {
//        this.diagnosticsDeltaManager = diagnosticsDeltaManager;
//        this.exprTrackers = new MemoMap<>();
//    }
//
//    private Collection<SourceCodeChange> getChangesBetweenFiles(DiagnosticsDeltaManager.SrcFilePair srcFiles) {
//        return exprTrackers.getOrInsert(srcFiles.oldFile, () -> {
//            FileDistiller distiller = ChangeDistiller.createFileDistiller(ChangeDistiller.Language.JAVA);
//            distiller.extractClassifiedSourceCodeChanges(
//                    srcFiles.oldFile.getTempFileOnDisk(), srcFiles.newFile.getTempFileOnDisk());
//            return distiller.getSourceCodeChanges();
//        });
//    }
//
//    private boolean doesChangeAffectDiagnostic(SourceCodeChange change, SrcFile file, DatasetDiagnostic diagnostic) {
//        SourceRange range = change.getChangedEntity().getSourceRange();
//        long position = file.getLineMap().getPosition(diagnostic.getLineNumber(), diagnostic.getColumnNumber());
//        return range.getStart() <= position && position <= range.getEnd();
//    }
//
//    private boolean diagnosticsMatchBecauseOfChange(SourceCodeChange change,
//                                                    DatasetDiagnostic oldDiagnostic,
//                                                    DatasetDiagnostic newDiagnostic,
//                                                    DiagnosticsDeltaManager.SrcFilePair srcFiles) {
//        String oldDiagSrc = srcFiles.oldFile.getSrcExtract(oldDiagnostic);
//        String newDiagSrc = srcFiles.newFile.getSrcExtract(newDiagnostic);
//
//        switch(change.getChangeType()) {
//            case STATEMENT_UPDATE:
//                Update update = (Update) change;
//                String oldUpdateSrc = srcFiles.oldFile.getSrcExtract(update.getChangedEntity().getSourceRange());
//                String newUpdateSrc = srcFiles.newFile.getSrcExtract(update.getNewEntity().getSourceRange());
//
//                return oldDiagSrc.equals(oldUpdateSrc) && newDiagSrc.equals(newUpdateSrc);
//        }
//        return false;
//    }
//
//    @Override
//    public boolean areSame(DatasetDiagnostic oldDiagnostic, DatasetDiagnostic newDiagnostic) {
//        if (!(diagnosticsDeltaManager.inSameFile(oldDiagnostic, newDiagnostic) && oldDiagnostic.isSameType(newDiagnostic))) {
//            return false;
//        }
//
//        try {
//            DiagnosticsDeltaManager.SrcFilePair srcFilePair = diagnosticsDeltaManager.loadFilesBetweenDiagnostics(oldDiagnostic, newDiagnostic);
//
//            Collection<SourceCodeChange> changes = getChangesBetweenFiles(srcFilePair);
//
//            return changes.stream()
//                    .filter(change -> doesChangeAffectDiagnostic(change, srcFilePair.oldFile, oldDiagnostic))
//                    .anyMatch(change ->
//                            diagnosticsMatchBecauseOfChange(change, oldDiagnostic,  newDiagnostic, srcFilePair));
//        } catch (IOException e) {
//            e.printStackTrace();
//            return false;
//        }
//    }
}
