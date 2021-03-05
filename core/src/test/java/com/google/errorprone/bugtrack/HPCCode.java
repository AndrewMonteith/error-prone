package com.google.errorprone.bugtrack;

import com.google.common.collect.ImmutableMap;
import com.google.errorprone.bugtrack.harness.LinesChangedCommitFilter;
import com.google.errorprone.bugtrack.harness.ProjectHarness;
import com.google.errorprone.bugtrack.harness.Verbosity;
import com.google.errorprone.bugtrack.projects.CorpusProject;
import com.google.errorprone.bugtrack.projects.JSoupProject;
import com.google.errorprone.bugtrack.projects.MetricsProject;
import com.google.errorprone.bugtrack.utils.GitUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.util.FS;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

public final class HPCCode {
    private static final Map<String, CorpusProject> projects = ImmutableMap.of(
            "jsoup", new JSoupProject(),
            "metrics", new MetricsProject()
    );

    @Before
    public void initJGit() {
        FS.DETECTED.setGitSystemConfig(Paths.get("/home/am2857/git/etc/gitconfig").toFile());
    }

    @Test
    public void scanCommit() throws IOException {
        System.out.println("Running");
        System.out.println(System.getenv("PATH"));
        CorpusProject project = projects.get(System.getProperty("project"));
        String sequenceNumAndCommit = System.getProperty("commit");

        String[] seqNumAndCommitSplit = sequenceNumAndCommit.split(" ");

        RevCommit commit = GitUtils.parseCommit(project.loadRepo(), seqNumAndCommitSplit[1]);
        Path outputFile = Paths.get(System.getProperty("outputFolder"), sequenceNumAndCommit);

        System.out.println("Parsing commit " + commit.getName() + " for project " + System.getProperty("project"));
        System.out.println("Saving response into " + outputFile.toString());

        new ProjectHarness(project, Verbosity.VERBOSE).serialiseCommit(commit, outputFile);
    }

    @Test
    public void genCommits() throws GitAPIException, IOException {
        Repository repo = projects.get(System.getProperty("project")).loadRepo();
        CommitRange range = new CommitRange(System.getProperty("oldCommit"), System.getProperty("newCommit"));

        List<RevCommit> filteredCommits = new LinesChangedCommitFilter(new Git(repo), 50)
                .filterCommits(GitUtils.expandCommitRange(repo, range));

        for (int i = 0; i < filteredCommits.size(); ++i) {
            System.out.println(i + " " + filteredCommits.get(i).getName());
        }
    }
}
