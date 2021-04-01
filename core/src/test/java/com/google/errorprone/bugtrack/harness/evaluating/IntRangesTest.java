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

package com.google.errorprone.bugtrack.harness.evaluating;

import org.junit.Assert;
import org.junit.Test;

public final class IntRangesTest {
  @Test
  public void correctlyQueriesMiddleOfRange() {
    Assert.assertTrue(IntRanges.include(1, 10).contains(1));
  }

  @Test
  public void correctlyQueriesEndpoints() {
    Assert.assertTrue(IntRanges.include(1, 10).contains(1));
    Assert.assertTrue(IntRanges.include(1, 10).contains(10));
  }

  @Test
  public void correctlyQueriesOutsideRange() {
    Assert.assertFalse(IntRanges.include(1, 10).contains(0));
    Assert.assertFalse(IntRanges.include(1, 10).contains(11));
  }

  @Test
  public void correctlyQueriesFragmentedRange() {
    IntRanges range = IntRanges.include(1, 10).excludeRange(3, 5);

    Assert.assertTrue(range.contains(2));
    Assert.assertFalse(range.contains(3));
    Assert.assertFalse(range.contains(4));
    Assert.assertFalse(range.contains(5));
    Assert.assertTrue(range.contains(6));
  }
}
