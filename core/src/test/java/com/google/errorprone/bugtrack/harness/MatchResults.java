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

import com.google.common.collect.Sets;
import com.google.errorprone.bugtrack.DatasetDiagnostic;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

public final class MatchResults {
    private final Map<DatasetDiagnostic, DatasetDiagnostic> matchedDiagnostics;
    private final Set<DatasetDiagnostic> unmatchedOld;
    private final Set<DatasetDiagnostic> unmatchedNew;

    public MatchResults(Collection<DatasetDiagnostic> oldDiagnostics,
                        Collection<DatasetDiagnostic> newDiagnostics,
                        Map<DatasetDiagnostic, DatasetDiagnostic> matchedDiagnostics) {
        this.matchedDiagnostics = matchedDiagnostics;
        this.unmatchedOld = Sets.difference(Sets.newHashSet(oldDiagnostics), matchedDiagnostics.keySet());
        this.unmatchedNew = Sets.difference(Sets.newHashSet(newDiagnostics), Sets.newHashSet(matchedDiagnostics.values()));
    }

    public Collection<DatasetDiagnostic> getOldDiagnostics() {
        return Sets.union(unmatchedOld, matchedDiagnostics.keySet());
    }

    public Collection<DatasetDiagnostic> getNewDiagnostics() {
        return Sets.union(unmatchedNew, Sets.newHashSet(matchedDiagnostics.values()));
    }

    public Collection<DatasetDiagnostic> getUnmatchedOldDiagnostics() {
        return unmatchedOld;
    }

    public Collection<DatasetDiagnostic> getUnmatchedNewDiagnostics() {
        return unmatchedNew;
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();

        result.append(String.format("There are %d old and %d new diagnostics\n", getOldDiagnostics().size(), getNewDiagnostics().size()));
        result.append(String.format("We matched %d old diagnostics\n", matchedDiagnostics.keySet().size()));
        result.append(String.format("We could not match %d old and %d new diagnostics\n", unmatchedOld.size(), unmatchedNew.size()));

        if (!unmatchedOld.isEmpty()) {
            result.append("Unmatched old diagnostics:\n");
            unmatchedOld.forEach(diag -> result.append(diag).append("\n"));
            result.append("\n");
        }

        result.append("Unmatched new diagnostics:\n");
        unmatchedNew.forEach(diag -> result.append(diag).append("\n"));

        return result.toString();
    }
}
