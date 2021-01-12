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

import com.google.common.collect.ImmutableList;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.List;

@RunWith(JUnit4.class)
public final class DiagnosticsDistributionTest {
    @Test
    public void recognisesEqualDistributions() {
        // GIVEN:
        List<String> diagnosticKinds = ImmutableList.of("MissingOverride", "OperatorPrecedence", "MissingOverride");
        List<String> diagnosticKinds2 = ImmutableList.of("OperatorPrecedence", "MissingOverride", "MissingOverride");

        // WHEN:
        DiagnosticsDistribution distribution = DiagnosticsDistribution.fromDiagnosticKinds(diagnosticKinds);
        DiagnosticsDistribution distribution2 = DiagnosticsDistribution.fromDiagnosticKinds(diagnosticKinds2);

        // THEN:
        Assert.assertEquals(distribution, distribution2);
        Assert.assertEquals(distribution2, distribution);
    }

    @Test
    public void recognisesUnequalDistributionsWithDifferentCardinality() {
        List<String> diagnosticKinds = ImmutableList.of("MissingOverride", "OperatorPrecedence", "Mutability");
        List<String> diagnosticKinds2 = ImmutableList.of("MissingOverride", "OperatorPrecedence");

        // WHEN:
        DiagnosticsDistribution distribution = DiagnosticsDistribution.fromDiagnosticKinds(diagnosticKinds);
        DiagnosticsDistribution distribution2 = DiagnosticsDistribution.fromDiagnosticKinds(diagnosticKinds2);

        // THEN:
        Assert.assertNotEquals(distribution, distribution2);
        Assert.assertNotEquals(distribution2, distribution);
    }

    @Test
    public void recognisesUnequalDistributionsWithSameCardinality() {
        List<String> diagnosticKinds = ImmutableList.of("MissingOverride", "OperatorPrecedence", "Mutability");
        List<String> diagnosticKinds2 = ImmutableList.of("MissingOverride", "OperatorPrecedence", "Immutability");

        // WHEN:
        DiagnosticsDistribution distribution = DiagnosticsDistribution.fromDiagnosticKinds(diagnosticKinds);
        DiagnosticsDistribution distribution2 = DiagnosticsDistribution.fromDiagnosticKinds(diagnosticKinds2);

        // THEN:
        Assert.assertNotEquals(distribution, distribution2);
        Assert.assertNotEquals(distribution2, distribution);
    }
}
