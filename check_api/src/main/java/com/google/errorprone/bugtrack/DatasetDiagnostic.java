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

package com.google.errorprone.bugtrack;

import com.google.errorprone.bugtrack.signatures.DiagnosticSignature;
import com.google.errorprone.bugtrack.signatures.SignatureBucket;

import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;

public class DatasetDiagnostic {
    private final String fileName;
    private final long lineNumber;
    private final long columnNumber;

    private final String message;
    private final DiagnosticSignature signature;

    public DatasetDiagnostic(String fileName, long lineNumber, long columnNumber, String message, DiagnosticSignature signature) {
        this.fileName = fileName;
        this.lineNumber = lineNumber;
        this.columnNumber = columnNumber;
        this.message = message;
        this.signature = signature;
    }

    public DatasetDiagnostic(String fileName, long lineNumber, long columnNumber, String message) {
        this(fileName, lineNumber, columnNumber, message, null);
    }

    public long getLineNumber() {
        return lineNumber;
    }

    public long getColumnNumber() {
        return columnNumber;
    }

    public String getFileName() {
        return fileName;
    }

    public String getMessage() {
        return message;
    }

    public DatasetDiagnostic(Diagnostic<? extends JavaFileObject> diagnostic) {
        this(diagnostic.getSource().getName(),
                diagnostic.getLineNumber(),
                diagnostic.getColumnNumber(),
                diagnostic.getMessage(null),
                SignatureBucket.DIAGNOSTIC_SIGNATURES.getOrDefault(diagnostic, null));
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();

        result.append("----DIAGNOSTIC\n");
        result.append(fileName).append(" ").append(lineNumber).append(" ").append(columnNumber).append("\n");
        result.append(message).append("\n");

        if (signature != null) {
            result.append("Signature ").append(signature).append("\n");
        }

        return result.toString();
    }

}
