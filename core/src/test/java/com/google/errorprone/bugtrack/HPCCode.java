 package com.google.errorprone.bugtrack;

 import com.google.common.collect.ImmutableMap;
 import com.google.errorprone.bugtrack.harness.ProjectHarness;
 import com.google.errorprone.bugtrack.harness.Verbosity;
 import com.google.errorprone.bugtrack.projects.CorpusProject;
 import com.google.errorprone.bugtrack.projects.JSoupProject;
 import com.google.errorprone.bugtrack.projects.MetricsProject;
 import com.google.errorprone.bugtrack.utils.GitUtils;
 import org.eclipse.jgit.api.errors.GitAPIException;
 import org.eclipse.jgit.revwalk.RevCommit;
 import org.eclipse.jgit.util.FS;
 import org.junit.Test;

 import java.io.IOException;
 import java.nio.file.Path;
 import java.nio.file.Paths;
 import java.util.Map;

 public final class HPCCode {
     private static final Map<String, CorpusProject> projects = ImmutableMap.of(
             "jsoup", new JSoupProject(),
             "metrics", new MetricsProject()
     );

     @Test
     public void scanCommit() throws GitAPIException, IOException, NoSuchFieldException, IllegalAccessException {
         FS.DETECTED.setGitSystemConfig(Paths.get("/home/am2857/git/etc/gitconfig").toFile());

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
 }
