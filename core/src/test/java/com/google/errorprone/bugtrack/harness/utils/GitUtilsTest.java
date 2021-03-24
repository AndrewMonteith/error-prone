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

import com.google.errorprone.bugtrack.CommitRange;
import com.google.errorprone.bugtrack.DatasetDiagnostic;
import com.google.errorprone.bugtrack.utils.GitUtils;
import com.google.errorprone.bugtrack.motion.SrcFile;
import com.google.errorprone.bugtrack.projects.JSoupProject;
import com.google.googlejavaformat.java.FormatterException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.IOException;
import java.util.List;

@RunWith(JUnit4.class)
public class GitUtilsTest {
    @Test
    public void canExpandValidCommitRange() throws GitAPIException {
        // GIVEN:
        Repository project = new JSoupProject().loadRepo();
        CommitRange range = new CommitRange("3c37bffe", "690d6019");

        // WHEN:
        List<RevCommit> expandedRange = GitUtils.expandCommitRange(project, range);

        // EXPECT:
        final String[] expectedHashes = new String[]{"3c37bffe", "afd73606", "724b2c5b", "690d6019"};
        for (int i = 0; i < expectedHashes.length; ++i) {
            Assert.assertTrue(expandedRange.get(i).getName().startsWith(expectedHashes[i]));
        }
    }

//    @Test
//    public void canLoadObjectsFromCommits() throws IOException {
//        // GIVEN:
//        Repository project = new TestProject().loadRepo();
//        RevCommit commit = GitUtils.parseCommit(project, "e4ffa7f74f461ca3e36fb89987f77e991ed8d998");
//        String file1 = "hi3.cpp", file2 = "foo/hi.txt";
//
//        // WHEN:
//        SrcFile file1Lines = GitUtils.loadSrcFile(project, commit, file1);
//        SrcFile file2Lines = GitUtils.loadSrcFile(project, commit, file2);
//
//        // THEN:
//        Assert.assertEquals(6, file1Lines.getLines().size());
//        Assert.assertEquals(5, file2Lines.getLines().size());
//    }

    @Test
    public void canLoadLineFromDiagnostic() throws IOException, FormatterException {
        // GIVEN:
        Repository repo = new JSoupProject().loadRepo();
        RevCommit commit = GitUtils.parseCommit(repo, "b61a3e6b340b878b30c518e35c6066f559b5102e");
        DatasetDiagnostic diag = new DatasetDiagnostic("src/test/java/org/jsoup/nodes/DocumentTest.java", 44, 0, "test");

        // WHEN:
        String line = GitUtils.loadJavaLine(repo, commit, diag).trim();

        // THEN:
        Assert.assertEquals("Document noTitle = Jsoup.parse(\"<p>Hello</p>\");", line);
    }
}
