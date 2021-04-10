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

package com.google.errorprone.bugtrack.utils;

import java.util.function.Predicate;

public interface ThrowingPredicate<T> extends Predicate<T> {
  default boolean test(T t) {
    try {
      return testThrows(t);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  boolean testThrows(T t) throws Exception;
}
