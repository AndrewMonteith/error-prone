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

package com.google.errorprone.bugtrack.harness;

import com.google.common.collect.Iterables;
import com.google.errorprone.bugtrack.DatasetDiagnostic;
import com.google.errorprone.bugtrack.DiagnosticUtils;
import com.google.errorprone.bugtrack.GitUtils;
import com.google.errorprone.bugtrack.projects.CorpusProject;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;

import javax.xml.crypto.Data;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.function.BiFunction;

import static com.google.errorprone.bugtrack.DiagnosticUtils.extractDiagnosticType;

public final class GitLineMissedDiagnosticsFinder {
    public static void findLikelyMissedDiagnostics(CorpusProject project,
                                                   RevCommit oldCommit,
                                                   RevCommit newCommit,
                                                   MatchResults results) throws IOException {

        Repository repo = project.loadRepo();
        /*
            Look for new diagnostics of the same type who's diagnostic contains a fragment of or looks similar to the old diagnostic.
         */
        for (DatasetDiagnostic unmatchedOldDiag : results.getUnmatchedOldDiagnostics()) {
            String oldLine = GitUtils.loadJavaLine(repo, oldCommit, unmatchedOldDiag);

        }
        results.getUnmatchedOldDiagnostics().forEach(unmatchedOldDiag -> {
//            String oldLine = GitUtilksgetLine(results. unmatchedOldDiag.getFileName(), unmatchedOldDiag.getLineNumber())



        });
    }

}
