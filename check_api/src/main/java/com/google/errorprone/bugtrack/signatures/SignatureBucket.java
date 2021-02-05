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

package com.google.errorprone.bugtrack.signatures;

import com.google.errorprone.VisitorState;
import com.google.errorprone.matchers.Description;

import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

// This is a necessary class to get around the design of error prone. Their design is good but I just need to abuse
// it slightly for my needs. May this code never see the light of day bar the mellow dim of my rooms lightbulb.

public class SignatureBucket {
    private static StringHasherMap<Diagnostic<? extends JavaFileObject>, DiagnosticSignature> DIAGNOSTIC_SIGNATURES =
        new StringHasherMap<>(Object::toString);

    public static DiagnosticSignature getSignature(Diagnostic<? extends JavaFileObject> diagnostic) {
        return DIAGNOSTIC_SIGNATURES.get(diagnostic);
    }

    private static StringHasherMap<Description, StateBucket> DESCRIPTION_STATES =
            new StringHasherMap<>(description -> description.checkName + " " + description.getRawMessage());

    public static void recordSignature(Diagnostic<? extends JavaFileObject> diagnostic, Description description) {
        if (!DESCRIPTION_STATES.containsKey(description)) {
            return;
        }
        DIAGNOSTIC_SIGNATURES.put(diagnostic, new TreeSignature(DESCRIPTION_STATES.get(description)));
    }

    public static void putState(Description description, VisitorState state) {
        DESCRIPTION_STATES.put(description, new StateBucket(state));
    }

    public static void clear() {
        DESCRIPTION_STATES.clear();
        DIAGNOSTIC_SIGNATURES.clear();
    }
}

class StringHasherMap<K, V> {
    private final Function<K, String> hashFunction;
    private final Map<String, V> hashMap;

    public StringHasherMap(Function<K, String> hashFunction) {
        this.hashFunction = hashFunction;
        this.hashMap = new HashMap<>();
    }

    public V get(K key) {
        return hashMap.get(hashFunction.apply(key));
    }

    public V take(K key) {
        String stringKey = hashFunction.apply(key);
        V object = hashMap.get(stringKey);
        hashMap.remove(stringKey);
        return object;
    }

    public boolean containsKey(K key) {
        return hashMap.containsKey(hashFunction.apply(key));
    }

    public void put(K key, V value) {
        hashMap.put(hashFunction.apply(key), value);
    }

    public void clear() {
        hashMap.clear();
    }
}
