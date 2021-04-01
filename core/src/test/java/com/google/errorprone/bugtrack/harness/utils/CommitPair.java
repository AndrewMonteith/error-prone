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

package com.google.errorprone.bugtrack.harness.utils;

import org.eclipse.jgit.revwalk.RevCommit;

import java.util.Objects;

public class CommitPair {
  public final RevCommit oldCommit;
  public final RevCommit newCommit;

  private CommitPair(final RevCommit oldCommit, final RevCommit newCommit) {
    this.oldCommit = oldCommit;
    this.newCommit = newCommit;
  }

  public static CommitPair of(final RevCommit oldCommit, final RevCommit newCommit) {
    return new CommitPair(oldCommit, newCommit);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    CommitPair that = (CommitPair) o;
    return oldCommit.equals(that.oldCommit) && newCommit.equals(that.newCommit);
  }

  @Override
  public int hashCode() {
    return Objects.hash(oldCommit, newCommit);
  }

  @Override
  public String toString() {
    return oldCommit.getName() + " -> " + newCommit.getName();
  }
}
