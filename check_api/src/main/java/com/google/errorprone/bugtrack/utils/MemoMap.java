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

import java.util.HashMap;
import java.util.function.Supplier;

public class MemoMap<K, V> extends HashMap<K, V> {
    @FunctionalInterface
    public interface SupplierWithException<T> {
        T get() throws Exception;
    }

    public V getOrInsert(K key, Supplier<V> defaultVal) {
        if (containsKey(key)) {
            return get(key);
        }

        put(key, defaultVal.get());
        return get(key);
    }

    public V getOrInsertThatCouldThrow(K key, SupplierWithException<V> defaultVal) throws Exception {
        if (containsKey(key)) {
            return get(key);
        }

        put(key, defaultVal.get());
        return get(key);
    }

}
