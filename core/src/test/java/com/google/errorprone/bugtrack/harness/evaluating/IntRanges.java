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

import com.google.common.collect.Sets;
import com.google.common.primitives.Ints;

import java.util.*;

public final class IntRanges {
    private final Range entireRange;
    private final List<Range> excludes;

    private IntRanges(final int start, final int end) {
        this.entireRange = new Range(start, end);
        this.excludes = new ArrayList<>();
    }

    public static IntRanges include(final int start, final int end) {
        return new IntRanges(start, end);
    }

    public static IntRanges specific(int... numbers) {
        List<Integer> numList = Ints.asList(Arrays.stream(numbers).sorted().toArray());

        int min = numList.stream().min(Comparator.naturalOrder()).get();
        int max = numList.stream().max(Comparator.naturalOrder()).get();

        IntRanges range = IntRanges.include(min, max);
        for (int i = min; i < max; ++i) {
            if (!numList.contains(i)) {
                range.exclude(i);
            }
        }

        return range;
    }

    public IntRanges excludeRange(int start, int end) {
        this.excludes.add(new Range(start, end));
        return this;
    }

    public boolean contains(int val) {
        return entireRange.isIn(val) && excludes.stream().noneMatch(r -> r.isIn(val));
    }

    public IntRanges exclude(int... numbers) {
        for (int i : numbers) {
            excludeRange(i, i);
        }

        return this;
    }

    private class Range {
        final int start;
        final int end;

        Range(final int start, final int end) {
            this.start = start;
            this.end = end;
        }

        public boolean isIn(int i) {
            return start <= i && i <= end;
        }
    }

}
