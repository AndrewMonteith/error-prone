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

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.errorprone.bugtrack.signatures.DiagnosticSignature;
import com.google.errorprone.bugtrack.signatures.SignatureBucket;
import com.google.errorprone.bugtrack.utils.DiagnosticUtils;
import com.google.errorprone.bugtrack.utils.ProjectFiles;

import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import java.util.List;
import java.util.Objects;

public class DatasetDiagnostic {
  private final String fileName;
  private final long lineNumber;
  private final long columnNumber;

  private final long startPos;
  private final long pos;
  private final long endPos;

  private final String message;
  private final String type;
  private final DiagnosticSignature signature;

  public DatasetDiagnostic(
      String fileName,
      long lineNumber,
      long columnNumber,
      long startPos,
      long pos,
      long endPos,
      String message,
      DiagnosticSignature signature) {
    this.fileName = fileName;
    this.lineNumber = lineNumber;
    this.columnNumber = columnNumber;
    this.startPos = startPos;
    this.pos = pos;
    this.endPos = endPos;
    this.message = message;
    this.type = DiagnosticUtils.extractDiagnosticType(message);
    this.signature = signature;
  }

  public DatasetDiagnostic(String fileName, long lineNumber, long columnNumber, String message) {
    this(fileName, lineNumber, columnNumber, -1, -1, -1, message, null);
  }

  public DatasetDiagnostic(
      String fileName,
      long lineNumber,
      long columnNumber,
      long startPos,
      long endPos,
      String message) {
    this(fileName, lineNumber, columnNumber, startPos, -1, endPos, message);
  }

  public DatasetDiagnostic(
      String fileName,
      long lineNumber,
      long columnNumber,
      long startPos,
      long pos,
      long endPos,
      String message) {
    this(fileName, lineNumber, columnNumber, startPos, pos, endPos, message, null);
  }

  public DatasetDiagnostic(Diagnostic<? extends JavaFileObject> diagnostic) {
    this(
        diagnostic.getSource().getName(),
        diagnostic.getLineNumber(),
        diagnostic.getColumnNumber(),
        diagnostic.getStartPosition(),
        diagnostic.getPosition(),
        diagnostic.getEndPosition(),
        diagnostic.getMessage(null),
        SignatureBucket.getSignature(diagnostic));
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

  public String getType() {
    return type;
  }

  public boolean isSameType(DatasetDiagnostic other) {
    return type.equals(other.type);
  }

  @Override
  public String toString() {
    StringBuilder result = new StringBuilder();

    result.append("----DIAGNOSTIC\n");
    result
        .append(fileName)
        .append(" ")
        .append(lineNumber)
        .append(" ")
        .append(columnNumber)
        .append(" ")
        .append(startPos)
        .append(" ")
        .append(pos)
        .append(" ")
        .append(endPos)
        .append("\n");
    result.append(message).append("\n");

    if (signature != null) {
      result.append("Signature ").append(signature).append("\n");
    }

    return result.toString();
  }

  public long getStartPos() {
    return startPos;
  }

  public long getEndPos() {
    return endPos;
  }

  public long getPos() {
    return pos;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    // Assumption: this & that are in the same file.
    DatasetDiagnostic that = (DatasetDiagnostic) o;
    return refersToSameSource(that) && fileName.equals(that.fileName);
  }

  public boolean refersToSameSource(DatasetDiagnostic other) {
    return lineNumber == other.lineNumber
        && columnNumber == other.columnNumber
        && startPos == other.startPos
        && pos == other.pos
        && endPos == other.endPos
        && message.equals(other.message);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        fileName, lineNumber, columnNumber, startPos, pos, endPos, message, fileName);
  }
}
