/*
 * Copyright 2018 The Error Prone Authors.
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
package com.google.errorprone.dataflow;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.mock;

import com.google.errorprone.dataflow.nullnesspropagation.Nullness;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.File;
import java.util.List;

/** @author bennostein@google.com (Benno Stein) */
@RunWith(JUnit4.class)
public class AccessPathStoreTest {

  @Test
  public void leastUpperBoundEmpty() {
    assertThat(newStore().leastUpperBound(newStore())).isEqualTo(newStore());
  }

//  @Test
//  public void foo() {
//    FileDistiller distiller = ChangeDistiller.createFileDistiller(ChangeDistiller.Language.JAVA);
//    File oldFile = new File("/home/monty/IdeaProjects/error-prone/core/src/test/java/com/google/errorprone/bugtrack/testdata/foo_4.java");
//    File newFile = new File("/home/monty/IdeaProjects/error-prone/core/src/test/java/com/google/errorprone/bugtrack/testdata/foo_5.java");
//
//    distiller.extractClassifiedSourceCodeChanges(oldFile, "1.8", newFile, "1.8");
//    List<SourceCodeChange> changes = distiller.getSourceCodeChanges();
//    if(changes != null) {
//      for(SourceCodeChange change : changes) {
//        System.out.println(change);
//        // see Javadocs for more information
//      }
//    }
//  }
//
  @Test
  public void buildAndGet() {
    AccessPathStore.Builder<Nullness> builder = newStore().toBuilder();
    AccessPath path1 = mock(AccessPath.class);
    AccessPath path2 = mock(AccessPath.class);
    builder.setInformation(path1, Nullness.NULL);
    builder.setInformation(path2, Nullness.NONNULL);
    assertThat(builder.build().valueOfAccessPath(path1, Nullness.BOTTOM)).isEqualTo(Nullness.NULL);
    assertThat(builder.build().valueOfAccessPath(path2, Nullness.BOTTOM))
        .isEqualTo(Nullness.NONNULL);
    assertThat(newStore().heap()).isEmpty();
  }

  private static AccessPathStore<Nullness> newStore() {
    return AccessPathStore.empty();
  }
}
