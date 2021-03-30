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

package com.google.errorprone.bugtrack.harness;

import com.google.errorprone.bugtrack.CommitRange;
import com.google.errorprone.bugtrack.utils.GitUtils;
import com.google.errorprone.bugtrack.projects.JSoupProject;
import org.eclipse.jgit.api.Git;
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
public final class LinesChangedCommitFilterTest {
    @Test
    public void lineChangeFilterNotTriggeredUnderThreshold() throws IOException, GitAPIException, InterruptedException {
        // GIVEN:
        Repository jsoupRepo = new JSoupProject().loadRepo();
        CommitRange range = new CommitRange("c0b275361ce8409f85de3573d89f6a617f0084d5", "62f5035bc6b466f59231c59a7ed414db8783c0c3");

        // WHEN:
        List<RevCommit> filteredCommits = new LinesChangedCommitFilter(new Git(jsoupRepo), 50)
                .filterCommits(GitUtils.expandCommitRange(jsoupRepo, range));

        // THEN:
        Assert.assertEquals(2, filteredCommits.size());
        Assert.assertTrue(filteredCommits.get(0).getName().startsWith("c0b27536"));
        Assert.assertTrue(filteredCommits.get(1).getName().startsWith("62f5035b"));
    }

    @Test
    public void lineChangeFilterTriggeredOverThreshold() throws IOException, GitAPIException, InterruptedException {
        // GIVEN:
        Repository jsoupRepo = new JSoupProject().loadRepo();
        CommitRange range = new CommitRange("c0b275361ce8409f85de3573d89f6a617f0084d5", "f6388656f6c1ce111bc2dd158bf5a3732bda0605");

        // WHEN:
        List<RevCommit> filteredCommits = new LinesChangedCommitFilter(new Git(jsoupRepo), 50)
                .filterCommits(GitUtils.expandCommitRange(jsoupRepo, range));

        // THEN:
        Assert.assertEquals(3, filteredCommits.size());
        Assert.assertTrue(filteredCommits.get(0).getName().startsWith("c0b27536"));
        Assert.assertTrue(filteredCommits.get(1).getName().startsWith("6e3c98c4"));
        Assert.assertTrue(filteredCommits.get(2).getName().startsWith("f6388656"));
    }
}
