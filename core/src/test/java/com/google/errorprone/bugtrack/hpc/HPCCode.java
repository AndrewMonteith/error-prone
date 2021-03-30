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

package com.google.errorprone.bugtrack.hpc;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.errorprone.bugtrack.CommitRange;
import com.google.errorprone.bugtrack.GitPathComparer;
import com.google.errorprone.bugtrack.GitSrcFilePairLoader;
import com.google.errorprone.bugtrack.harness.LinesChangedCommitFilter;
import com.google.errorprone.bugtrack.harness.ProjectHarness;
import com.google.errorprone.bugtrack.harness.Verbosity;
import com.google.errorprone.bugtrack.harness.evaluating.*;
import com.google.errorprone.bugtrack.harness.scanning.DiagnosticsCollector;
import com.google.errorprone.bugtrack.harness.scanning.DiagnosticsScan;
import com.google.errorprone.bugtrack.projects.*;
import com.google.errorprone.bugtrack.utils.GitUtils;
import com.google.errorprone.bugtrack.utils.ProjectFiles;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.util.FS;
import org.junit.Before;
import org.junit.Test;

import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.google.errorprone.bugtrack.harness.evaluating.BugComparerExperiment.withGit;
import static com.google.errorprone.bugtrack.motion.trackers.DPTrackerConstructorFactory.*;

public final class HPCCode {
    private static final Map<String, CorpusProject> projects;

    static {
        Map<String, CorpusProject> projs = new HashMap<>();
        projs.put("jsoup", new JSoupProject());
        projs.put("metrics", new MetricsProject());
        projs.put("checkstyle", new CheckstyleProject());
        projs.put("junit4", new JUnitProject());
        projs.put("dubbo", new DubboProject());
        projs.put("guice", new GuiceProject());
        projs.put("mybatis-3", new MyBatis3Project());
        projs.put("jetty.project", new JettyProject());
        projs.put("hazelcast", new HazelcastProject());
        projs.put("zxing", new ZXingProject());
        projs.put("mcMMO", new McMMOProject());
        projs.put("cobertura", new CoberturaProject());
        projs.put("jruby", new JRubyProject());

        projects = ImmutableMap.copyOf(projs);
    }

    public static void main(String[] args) throws GitAPIException, IOException {
        Repository repo = new JRubyProject().loadRepo();
        CommitRange range = new CommitRange("77d1af438a16fc8795446b63644cc63a25b32e06", "45a5f884a1a001493a67c240180182c646ff8a38");

        System.out.println(GitUtils.expandCommitRange(repo, range).size());

        List<RevCommit> filteredCommits = new LinesChangedCommitFilter(new Git(repo), 200)
                .filterCommits(GitUtils.expandCommitRange(repo, range));

        System.out.println("Total commits  " + filteredCommits.size());
    }

    @Before
    public void initJGit() {
        FS.DETECTED.setGitSystemConfig(Paths.get("/rds/user/am2857/hpc-work/java-corpus/git/etc/gitconfig").toFile());
    }

    private Path getPath(String path1, String... paths) {
        Path p = Paths.get(path1, paths);
        if (!p.toFile().exists()) {
            throw new RuntimeException(p.toString() + " does not exist");
        }
        return p;
    }

    private CorpusProject loadProject() {
        String projectName = System.getProperty("project");
        if (!projects.containsKey(projectName)) {
            throw new RuntimeException("no project named " + projectName);
        }

        return new NewRootProject(projects.get(projectName), getPath(System.getProperty("projDir")));
    }

    @Test
    public void scanCommit() throws IOException {
        CorpusProject project = loadProject();

        String sequenceNumAndCommit = System.getProperty("commit");
        String[] seqNumAndCommitSplit = sequenceNumAndCommit.split(" ");

        RevCommit commit = GitUtils.parseCommit(project.loadRepo(), seqNumAndCommitSplit[1]);
        Path outputFile = getPath(System.getProperty("outputFolder")).resolve(sequenceNumAndCommit);

        System.out.println("Parsing commit " + commit.getName() + " for project " + System.getProperty("project"));
        System.out.println("Saving response into " + outputFile.toString());

        new ProjectHarness(project, Verbosity.VERBOSE).serialiseCommit(commit, outputFile);
    }

    @Test
    public void genCommits() throws GitAPIException, IOException {
        Repository repo = projects.get(System.getProperty("project")).loadRepo();
        CommitRange range = new CommitRange(System.getProperty("oldCommit"), System.getProperty("newCommit"));

        List<RevCommit> filteredCommits = new LinesChangedCommitFilter(new Git(repo), Integer.parseInt(System.getProperty("linesChanged")))
                .filterCommits(GitUtils.expandCommitRange(repo, range));

        System.out.println("Total commits  " + filteredCommits.size());

        for (int i = 0; i < filteredCommits.size(); ++i) {
            System.out.println(i + " " + filteredCommits.get(i).getName());
        }
    }

    @Test
    public void betweenCommits() throws GitAPIException, IOException {
        CorpusProject project = loadProject();
        CommitRange range = new CommitRange(System.getProperty("oldCommit"), System.getProperty("newCommit"));

        int linesChangedThreshold = System.getProperty("linesChanged") == null ? 50 : Integer.parseInt(System.getProperty("linesChanged"));

        System.out.println("Lines changed threshold " + linesChangedThreshold);
        System.out.println("Number of cores " + Runtime.getRuntime().availableProcessors());

        new ProjectHarness(project).serialiseCommits(range,
                new LinesChangedCommitFilter(new Git(project.loadRepo()), linesChangedThreshold),
                getPath(System.getProperty("outputFolder")),
                Integer.parseInt(System.getProperty("offset")));
    }

    @Test
    public void genDubboComparisons() throws Exception {
        CorpusProject project = new MyBatis3Project();
        Path diagFolders = ProjectFiles.get("diagnostics/dubbo");
        IntRanges validSeqFiles = IntRanges.include(0, 158).excludeRange(71, 73);

        BugComparerExperiment.forProject(project)
                .withData(LiveDatasetFilePairLoader.inSeqNumRange(diagFolders, validSeqFiles))
                .comparePaths(withGit(project, GitPathComparer::new))
                .loadDiags(withGit(project, GitSrcFilePairLoader::new))
                .makeBugComparer1(any(newTokenizedLineTracker(), newIJMStartPosTracker()))
                .makeBugComparer2(any(newTokenizedLineTracker(), newIJMStartAndEndTracker())//        files.forEach(projFile -> helper.addSourceFile(projFile.toFile().toPath()));
//        helper.setArgs(ImmutableList.copyOf(Iterables.concat(scan.cmdLineArguments, ImmutableList.of("-Xjcov"))));
//
//        return ListUtils.distinct(helper.collectDiagnostics());
)
                .findMissedTrackings(MissedLikelihoodCalculatorFactory.diagLineSrcOverlap())
                .trials(Integer.parseInt(System.getProperty("trials")))
                .run(ProjectFiles.get("comparisons/dubbo").toString());
    }

    @Test
    public void performSequentialComparisons() throws Exception {
        String projectName = System.getProperty("project");
        CorpusProject project = loadProject();

        List<GrainDiagFile> grainFiles = GrainDiagFile.loadSortedFiles(
                project, ProjectFiles.get("diagnostics/").resolve(projectName + "_full"));

        Path output = ProjectFiles.get("comparison_data/").resolve(projectName);

        if (System.getProperty("inParallel").equals("true")) {
            MultiGrainDiagFileComparer.compareFilesInParallel(project, output, grainFiles);
        } else {
            MultiGrainDiagFileComparer.compareFiles(project, output, grainFiles);
        }
    }

    @Test
    public void test() throws IOException {
        String oldCommit = "24393f7a05ec7304e744d62a90c781156e37d7e3";
        CorpusProject project = new HazelcastProject();

//        DiagnosticsScan scan = new DiagnosticsScan("default-compile",
//                ImmutableList.of(
//new ProjectFile(new HazelcastProject(), "hazelcast/src/main/java/com/hazelcast/map/impl/MapClientAwareService.java"),
//new ProjectFile(new HazelcastProject(), "hazelcast/src/main/java/com/hazelcast/map/impl/MapServiceContextInterceptorSupport.java"),
//new ProjectFile(new HazelcastProject(), "hazelcast/src/main/java/com/hazelcast/map/impl/BinaryMapEntryCostEstimator.java"),
//new ProjectFile(new HazelcastProject(), "hazelcast/src/main/java/com/hazelcast/map/impl/MapServiceContextImpl.java"),
//new ProjectFile(new HazelcastProject(), "hazelcast/src/main/java/com/hazelcast/map/impl/LazyEntryView.java"),
//new ProjectFile(new HazelcastProject(), "hazelcast/src/main/java/com/hazelcast/map/impl/MapManagedService.java"),
//new ProjectFile(new HazelcastProject(), "hazelcast/src/main/java/com/hazelcast/map/impl/MapEntrySimple.java"),
//new ProjectFile(new HazelcastProject(), "hazelcast/src/main/java/com/hazelcast/map/impl/EventListenerFilter.java"),
//new ProjectFile(new HazelcastProject(), "hazelcast/src/main/java/com/hazelcast/map/impl/MapKeyLoaderUtil.java"),
//new ProjectFile(new HazelcastProject(), "hazelcast/src/main/java/com/hazelcast/map/impl/mapstore/writethrough/WriteThroughStore.java"),
//new ProjectFile(new HazelcastProject(), "hazelcast/src/main/java/com/hazelcast/map/impl/mapstore/writethrough/WriteThroughManager.java"),
//new ProjectFile(new HazelcastProject(), "hazelcast/src/main/java/com/hazelcast/map/impl/mapstore/writethrough/package-info.java"),
//new ProjectFile(new HazelcastProject(), "hazelcast/src/main/java/com/hazelcast/map/impl/mapstore/StoreConstructor.java"),
//new ProjectFile(new HazelcastProject(), "hazelcast/src/main/java/com/hazelcast/map/impl/mapstore/BasicMapStoreContext.java"),
//new ProjectFile(new HazelcastProject(), "hazelcast/src/main/java/com/hazelcast/map/impl/mapstore/writebehind/WriteBehindProcessors.java"),
//new ProjectFile(new HazelcastProject(), "hazelcast/src/main/java/com/hazelcast/map/impl/mapstore/writebehind/DefaultWriteBehindProcessor.java"),
//new ProjectFile(new HazelcastProject(), "hazelcast/src/main/java/com/hazelcast/map/impl/mapstore/writebehind/StoreEvent.java"),
//new ProjectFile(new HazelcastProject(), "hazelcast/src/main/java/com/hazelcast/map/impl/mapstore/writebehind/CyclicWriteBehindQueue.java"),
//new ProjectFile(new HazelcastProject(), "hazelcast/src/main/java/com/hazelcast/map/impl/mapstore/writebehind/WriteBehindQueues.java"),
//new ProjectFile(new HazelcastProject(), "hazelcast/src/main/java/com/hazelcast/map/impl/mapstore/writebehind/entry/DelayedEntry.java"),
//new ProjectFile(new HazelcastProject(), "hazelcast/src/main/java/com/hazelcast/map/impl/mapstore/writebehind/entry/AddedDelayedEntry.java"),
//new ProjectFile(new HazelcastProject(), "hazelcast/src/main/java/com/hazelcast/map/impl/mapstore/writebehind/entry/DeletedDelayedEntry.java"),
//new ProjectFile(new HazelcastProject(), "hazelcast/src/main/java/com/hazelcast/map/impl/mapstore/writebehind/entry/NullValueDelayedEntry.java"),
//new ProjectFile(new HazelcastProject(), "hazelcast/src/main/java/com/hazelcast/map/impl/mapstore/writebehind/entry/DelayedEntries.java"),
//new ProjectFile(new HazelcastProject(), "hazelcast/src/main/java/com/hazelcast/map/impl/mapstore/writebehind/WriteBehindQueue.java"),
//new ProjectFile(new HazelcastProject(), "hazelcast/src/main/java/com/hazelcast/map/impl/mapstore/writebehind/StoreListener.java"),
//new ProjectFile(new HazelcastProject(), "hazelcast/src/main/java/com/hazelcast/map/impl/mapstore/writebehind/WriteBehindStore.java"),
//new ProjectFile(new HazelcastProject(), "hazelcast/src/main/java/com/hazelcast/map/impl/mapstore/writebehind/SynchronizedWriteBehindQueue.java"),
//new ProjectFile(new HazelcastProject(), "hazelcast/src/main/java/com/hazelcast/map/impl/mapstore/writebehind/WriteBehindProcessor.java"),
//new ProjectFile(new HazelcastProject(), "hazelcast/src/main/java/com/hazelcast/map/impl/mapstore/writebehind/CoalescedWriteBehindQueue.java"),
//new ProjectFile(new HazelcastProject(), "hazelcast/src/main/java/com/hazelcast/map/impl/mapstore/writebehind/AbstractWriteBehindProcessor.java"),
//new ProjectFile(new HazelcastProject(), "hazelcast/src/main/java/com/hazelcast/map/impl/mapstore/writebehind/StoreWorker.java"),
//new ProjectFile(new HazelcastProject(), "hazelcast/src/main/java/com/hazelcast/map/impl/mapstore/writebehind/package-info.java"),
//new ProjectFile(new HazelcastProject(), "hazelcast/src/main/java/com/hazelcast/map/impl/mapstore/writebehind/IPredicate.java"),
//new ProjectFile(new HazelcastProject(), "hazelcast/src/main/java/com/hazelcast/map/impl/mapstore/writebehind/BoundedWriteBehindQueue.java"),
//new ProjectFile(new HazelcastProject(), "hazelcast/src/main/java/com/hazelcast/map/impl/mapstore/writebehind/WriteBehindManager.java"),
//new ProjectFile(new HazelcastProject(), "hazelcast/src/main/java/com/hazelcast/map/impl/mapstore/MapDataStore.java"),
//new ProjectFile(new HazelcastProject(), "hazelcast/src/main/java/com/hazelcast/map/impl/mapstore/MapStoreContext.java"),
//new ProjectFile(new HazelcastProject(), "hazelcast/src/main/java/com/hazelcast/map/impl/mapstore/MapStoreManagers.java"),
//new ProjectFile(new HazelcastProject(), "hazelcast/src/main/java/com/hazelcast/map/impl/mapstore/AbstractMapDataStore.java"),
//new ProjectFile(new HazelcastProject(), "hazelcast/src/main/java/com/hazelcast/map/impl/mapstore/MapStoreManager.java"),
//new ProjectFile(new HazelcastProject(), "hazelcast/src/main/java/com/hazelcast/map/impl/mapstore/EmptyMapDataStore.java"),
//new ProjectFile(new HazelcastProject(), "hazelcast/src/main/java/com/hazelcast/map/impl/mapstore/MapStoreContextFactory.java"),
//new ProjectFile(new HazelcastProject(), "hazelcast/src/main/java/com/hazelcast/map/impl/mapstore/package-info.java"),
//new ProjectFile(new HazelcastProject(), "hazelcast/src/main/java/com/hazelcast/map/impl/mapstore/MapDataStores.java"),
//new ProjectFile(new HazelcastProject(), "hazelcast/src/main/java/com/hazelcast/map/impl/OwnedEntryCostEstimatorFactory.java"),
//new ProjectFile(new HazelcastProject(), "hazelcast/src/main/java/com/hazelcast/map/impl/LegacyAsyncMap.java"),
//new ProjectFile(new HazelcastProject(), "hazelcast/src/main/java/com/hazelcast/map/impl/journal/RingbufferMapEventJournalImpl.java"),
//new ProjectFile(new HazelcastProject(), "hazelcast/src/main/java/com/hazelcast/map/impl/journal/InternalEventJournalMapEvent.java"),
//new ProjectFile(new HazelcastProject(), "hazelcast/src/main/java/com/hazelcast/map/impl/journal/MapEventJournal.java"),
//new ProjectFile(new HazelcastProject(), "hazelcast/src/main/java/com/hazelcast/map/impl/journal/DeserializingEventJournalMapEvent.java"),
//new ProjectFile(new HazelcastProject(), "hazelcast/src/main/java/com/hazelcast/map/impl/journal/MapEventJournalReadOperation.java"),
//new ProjectFile(new HazelcastProject(), "hazelcast/src/main/java/com/hazelcast/map/impl/journal/MapEventJournalReadResultSetImpl.java"),
//new ProjectFile(new HazelcastProject(), "hazelcast/src/main/java/com/hazelcast/map/impl/journal/MapEventJournalSubscribeOperation.java"),
//new ProjectFile(new HazelcastProject(), "hazelcast/src/main/java/com/hazelcast/map/impl/query/QueryEventFilter.java"),
//new ProjectFile(new HazelcastProject(), "hazelcast/src/main/java/com/hazelcast/map/impl/query/PartitionScanExecutor.java"),
//new ProjectFile(new HazelcastProject(), "hazelcast/src/main/java/com/hazelcast/map/impl/query/QueryOperation.java"),
//new ProjectFile(new HazelcastProject(), "hazelcast/src/main/java/com/hazelcast/map/impl/query/ResultProcessor.java"),
//new ProjectFile(new HazelcastProject(), "hazelcast/src/main/java/com/hazelcast/map/impl/query/AccumulationExecutor.java"),
//new ProjectFile(new HazelcastProject(), "hazelcast/src/main/java/com/hazelcast/map/impl/query/QueryResultSizeLimiter.java"),
//new ProjectFile(new HazelcastProject(), "hazelcast/src/main/java/com/hazelcast/map/impl/query/QueryResultIterator.java"),
//new ProjectFile(new HazelcastProject(), "hazelcast/src/main/java/com/hazelcast/map/impl/query/Result.java"),
//new ProjectFile(new HazelcastProject(), "hazelcast/src/main/java/com/hazelcast/map/impl/query/QueryEngineImpl.java"),
//new ProjectFile(new HazelcastProject(), "hazelcast/src/main/java/com/hazelcast/map/impl/query/QueryEntryFactory.java"),
//new ProjectFile(new HazelcastProject(), "hazelcast/src/main/java/com/hazelcast/map/impl/query/ParallelPartitionScanExecutor.java"),
//new ProjectFile(new HazelcastProject(), "hazelcast/src/main/java/com/hazelcast/map/impl/query/QueryRunner.java"),
//new ProjectFile(new HazelcastProject(), "hazelcast/src/main/java/com/hazelcast/map/impl/query/ParallelAccumulationExecutor.java"),
//new ProjectFile(new HazelcastProject(), "hazelcast/src/main/java/com/hazelcast/map/impl/query/QueryResult.java"),
//new ProjectFile(new HazelcastProject(), "hazelcast/src/main/java/com/hazelcast/map/impl/query/QueryEngine.java"),
//new ProjectFile(new HazelcastProject(), "hazelcast/src/main/java/com/hazelcast/map/impl/query/Query.java"),
//new ProjectFile(new HazelcastProject(), "hazelcast/src/main/java/com/hazelcast/map/impl/query/Target.java"),
//new ProjectFile(new HazelcastProject(), "hazelcast/src/main/java/com/hazelcast/map/impl/query/ResultProcessorRegistry.java"),
//new ProjectFile(new HazelcastProject(), "hazelcast/src/main/java/com/hazelcast/map/impl/query/ResultSegment.java"),
//new ProjectFile(new HazelcastProject(), "hazelcast/src/main/java/com/hazelcast/map/impl/query/QueryResultProcessor.java"),
//new ProjectFile(new HazelcastProject(), "hazelcast/src/main/java/com/hazelcast/map/impl/query/CallerRunsAccumulationExecutor.java"),
//new ProjectFile(new HazelcastProject(), "hazelcast/src/main/java/com/hazelcast/map/impl/query/QueryResultRow.java"),
//new ProjectFile(new HazelcastProject(), "hazelcast/src/main/java/com/hazelcast/map/impl/query/PartitionScanRunner.java"),
//new ProjectFile(new HazelcastProject(), "hazelcast/src/main/java/com/hazelcast/map/impl/query/QueryResultCollection.java"),
//new ProjectFile(new HazelcastProject(), "hazelcast/src/main/java/com/hazelcast/map/impl/query/AggregationResultProcessor.java"),
//new ProjectFile(new HazelcastProject(), "hazelcast/src/main/java/com/hazelcast/map/impl/query/QueryPartitionOperation.java"),
//new ProjectFile(new HazelcastProject(), "hazelcast/src/main/java/com/hazelcast/map/impl/query/AggregationResult.java"),
//new ProjectFile(new HazelcastProject(), "hazelcast/src/main/java/com/hazelcast/map/impl/query/QueryResultUtils.java"),
//new ProjectFile(new HazelcastProject(), "hazelcast/src/main/java/com/hazelcast/map/impl/query/CallerRunsPartitionScanExecutor.java"),
//new ProjectFile(new HazelcastProject(), "hazelcast/src/main/java/com/hazelcast/map/impl/LazyMapEntry.java"),
//new ProjectFile(new HazelcastProject(), "hazelcast/src/main/java/com/hazelcast/map/impl/InterceptorRegistry.java"),
//new ProjectFile(new HazelcastProject(), "hazelcast/src/main/java/com/hazelcast/map/impl/DataAwareEntryEvent.java"),
//new ProjectFile(new HazelcastProject(), "hazelcast/src/main/java/com/hazelcast/map/impl/client/MapPortableHook.java"),
//new ProjectFile(new HazelcastProject(), "hazelcast/src/main/java/com/hazelcast/map/impl/MapValueCollection.java"),
//new ProjectFile(new HazelcastProject(), "hazelcast/src/main/java/com/hazelcast/map/impl/PartitionContainer.java"),
//new ProjectFile(new HazelcastProject(), "hazelcast/src/main/java/com/hazelcast/map/impl/MapMergeRunnable.java"),
//new ProjectFile(new HazelcastProject(), "hazelcast/src/main/java/com/hazelcast/map/impl/MapKeyLoader.java"),
//new ProjectFile(new HazelcastProject(), "hazelcast/src/main/java/com/hazelcast/map/impl/EntryViews.java"),
//new ProjectFile(new HazelcastProject(), "hazelcast/src/main/java/com/hazelcast/map/impl/operation/EntryOperation.java"),
//new ProjectFile(new HazelcastProject(), "hazelcast/src/main/java/com/hazelcast/map/impl/operation/ContainsValueOperationFactory.java"),
//new ProjectFile(new HazelcastProject(), "hazelcast/src/main/java/com/hazelcast/map/impl/operation/MapPartitionDestroyOperation.java"),
//new ProjectFile(new HazelcastProject(), "hazelcast/src/main/java/com/hazelcast/map/impl/operation/KeyBasedMapOperation.java"),
//new ProjectFile(new HazelcastProject(), "hazelcast/src/main/java/com/hazelcast/map/impl/operation/MapIsEmptyOperation.java"),
//new ProjectFile(new HazelcastProject(), "hazelcast/src/main/java/com/hazelcast/map/impl/operation/EvictAllOperation.java"),
//new ProjectFile(new HazelcastProject(), "hazelcast/src/main/java/com/hazelcast/map/impl/operation/EvictBatchBackupOperation.java"),
//new ProjectFile(new HazelcastProject(), "hazelcast/src/main/java/com/hazelcast/map/impl/operation/RemoveFromLoadAllOperation.java"),
//new ProjectFile(new HazelcastProject(), "hazelcast/src/main/java/com/hazelcast/map/impl/operation/MultipleEntryWithPredicateBackupOperation.java"),
//new ProjectFile(new HazelcastProject(), "hazelcast/src/main/java/com/hazelcast/map/impl/operation/WANAwareOperationProvider.java"),
//new ProjectFile(new HazelcastProject(), "hazelcast/src/main/java/com/hazelcast/map/impl/operation/RemoveOperation.java"),
//new ProjectFile(new HazelcastProject(), "hazelcast/src/main/java/com/hazelcast/map/impl/operation/GetAllOperation.java"),
//new ProjectFile(new HazelcastProject(), "hazelcast/src/main/java/com/hazelcast/map/impl/operation/MultipleEntryOperation.java"),
//new ProjectFile(new HazelcastProject(), "hazelcast/src/main/java/com/hazelcast/map/impl/operation/IsEmptyOperationFactory.java"),
//new ProjectFile(new HazelcastProject(), "hazelcast/src/main/java/com/hazelcast/map/impl/operation/ReplaceOperation.java"),
//new ProjectFile(new HazelcastProject(), "hazelcast/src/main/java/com/hazelcast/map/impl/operation/EvictOperation.java"),
//new ProjectFile(new HazelcastProject(), "hazelcast/src/main/java/com/hazelcast/map/impl/operation/AddInterceptorOperation.java"),
//new ProjectFile(new HazelcastProject(), "hazelcast/src/main/java/com/hazelcast/map/impl/operation/MultipleEntryOperationFactory.java"),
//new ProjectFile(new HazelcastProject(), "hazelcast/src/main/java/com/hazelcast/map/impl/operation/MapNearCacheStateHolder.java"),
//new ProjectFile(new HazelcastProject(), "hazelcast/src/main/java/com/hazelcast/map/impl/operation/MapFlushOperationFactory.java"),
//new ProjectFile(new HazelcastProject(), "hazelcast/src/main/java/com/hazelcast/map/impl/operation/AddIndexOperationFactory.java"),
//new ProjectFile(new HazelcastProject(), "hazelcast/src/main/java/com/hazelcast/map/impl/operation/MapLoadAllOperationFactory.java"),
//new ProjectFile(new HazelcastProject(), "hazelcast/src/main/java/com/hazelcast/map/impl/operation/ContainsValueOperation.java"),
//new ProjectFile(new HazelcastProject(), "hazelcast/src/main/java/com/hazelcast/map/impl/operation/MapOperationProviders.java"),
//new ProjectFile(new HazelcastProject(), "hazelcast/src/main/java/com/hazelcast/map/impl/operation/AwaitMapFlushOperation.java"),
//new ProjectFile(new HazelcastProject(), "hazelcast/src/main/java/com/hazelcast/map/impl/operation/EntryOffloadableSetUnlockOperation.java"),
//new ProjectFile(new HazelcastProject(), "hazelcast/src/main/java/com/hazelcast/map/impl/operation/MapOperation.java"),
//new ProjectFile(new HazelcastProject(), "hazelcast/src/main/java/com/hazelcast/map/impl/operation/IsKeyLoadFinishedOperation.java"),
//new ProjectFile(new HazelcastProject(), "hazelcast/src/main/java/com/hazelcast/map/impl/operation/PartitionWideEntryWithPredicateOperationFactory.java"),
//new ProjectFile(new HazelcastProject(), "hazelcast/src/main/java/com/hazelcast/map/impl/operation/SetTtlBackupOperation.java"),
//new ProjectFile(new HazelcastProject(), "hazelcast/src/main/java/com/hazelcast/map/impl/operation/PutFromLoadAllOperation.java"),
//new ProjectFile(new HazelcastProject(), "hazelcast/src/main/java/com/hazelcast/map/impl/operation/EntryOffloadableLockMismatchException.java"),
//new ProjectFile(new HazelcastProject(), "hazelcast/src/main/java/com/hazelcast/map/impl/operation/AbstractMapOperationFactory.java"),
//new ProjectFile(new HazelcastProject(), "hazelcast/src/main/java/com/hazelcast/map/impl/operation/SizeOperationFactory.java"),
//new ProjectFile(new HazelcastProject(), "hazelcast/src/main/java/com/hazelcast/map/impl/operation/MultipleEntryWithPredicateOperation.java"),
//new ProjectFile(new HazelcastProject(), "hazelcast/src/main/java/com/hazelcast/map/impl/operation/EvictAllBackupOperation.java"),
//new ProjectFile(new HazelcastProject(), "hazelcast/src/main/java/com/hazelcast/map/impl/operation/MapOperationProviderDelegator.java"),
//new ProjectFile(new HazelcastProject(), "hazelcast/src/main/java/com/hazelcast/map/impl/operation/MapFetchKeysOperation.java"),
//new ProjectFile(new HazelcastProject(), "hazelcast/src/main/java/com/hazelcast/map/impl/operation/PostJoinMapOperation.java"),
//new ProjectFile(new HazelcastProject(), "hazelcast/src/main/java/com/hazelcast/map/impl/operation/PutAllPartitionAwareOperationFactory.java"),
//new ProjectFile(new HazelcastProject(), "hazelcast/src/main/java/com/hazelcast/map/impl/operation/PartitionWideEntryOperationFactory.java"),
//new ProjectFile(new HazelcastProject(), "hazelcast/src/main/java/com/hazelcast/map/impl/operation/ContainsKeyOperation.java"),
//new ProjectFile(new HazelcastProject(), "hazelcast/src/main/java/com/hazelcast/map/impl/operation/LockAwareOperation.java"),
//new ProjectFile(new HazelcastProject(), "hazelcast/src/main/java/com/hazelcast/map/impl/operation/LoadMapOperation.java"),
//new ProjectFile(new HazelcastProject(), "hazelcast/src/main/java/com/hazelcast/map/impl/operation/AddIndexOperation.java"),
//new ProjectFile(new HazelcastProject(), "hazelcast/src/main/java/com/hazelcast/map/impl/operation/MultipleEntryBackupOperation.java"),
//new ProjectFile(new HazelcastProject(), "hazelcast/src/main/java/com/hazelcast/map/impl/operation/KeyLoadStatusOperationFactory.java"),
//new ProjectFile(new HazelcastProject(), "hazelcast/src/main/java/com/hazelcast/map/impl/operation/PartitionWideEntryWithPredicateBackupOperation.java"),
//new ProjectFile(new HazelcastProject(), "hazelcast/src/main/java/com/hazelcast/map/impl/operation/WriteBehindStateHolder.java"),
//new ProjectFile(new HazelcastProject(), "hazelcast/src/main/java/com/hazelcast/map/impl/operation/BaseRemoveOperation.java"),
//new ProjectFile(new HazelcastProject(), "hazelcast/src/main/java/com/hazelcast/map/impl/operation/BasePutOperation.java"),
//new ProjectFile(new HazelcastProject(), "hazelcast/src/main/java/com/hazelcast/map/impl/operation/EvictAllOperationFactory.java"),
//new ProjectFile(new HazelcastProject(), "hazelcast/src/main/java/com/hazelcast/map/impl/operation/ClearExpiredOperation.java"),
//new ProjectFile(new HazelcastProject(), "hazelcast/src/main/java/com/hazelcast/map/impl/operation/TryRemoveOperation.java"),
//new ProjectFile(new HazelcastProject(), "hazelcast/src/main/java/com/hazelcast/map/impl/operation/ReadonlyKeyBasedMapOperation.java"),
//new ProjectFile(new HazelcastProject(), "hazelcast/src/main/java/com/hazelcast/map/impl/operation/TryPutOperation.java"),
//new ProjectFile(new HazelcastProject(), "hazelcast/src/main/java/com/hazelcast/map/impl/operation/IsPartitionLoadedOperation.java"),
//new ProjectFile(new HazelcastProject(), "hazelcast/src/main/java/com/hazelcast/map/impl/operation/ClearOperationFactory.java"),
//new ProjectFile(new HazelcastProject(), "hazelcast/src/main/java/com/hazelcast/map/impl/operation/MapFlushWaitNotifyKey.java"),
//new ProjectFile(new HazelcastProject(), "hazelcast/src/main/java/com/hazelcast/map/impl/operation/AccumulatorConsumerOperation.java"),
//new ProjectFile(new HazelcastProject(), "hazelcast/src/main/java/com/hazelcast/map/impl/operation/LegacyMergeOperation.java"),
//new ProjectFile(new HazelcastProject(), "hazelcast/src/main/java/com/hazelcast/map/impl/operation/MapFetchEntriesOperation.java"),
//new ProjectFile(new HazelcastProject(), "hazelcast/src/main/java/com/hazelcast/map/impl/operation/MapReplicationOperation.java"),
//new ProjectFile(new HazelcastProject(), "hazelcast/src/main/java/com/hazelcast/map/impl/operation/PutAllBackupOperation.java"),
//new ProjectFile(new HazelcastProject(), "hazelcast/src/main/java/com/hazelcast/map/impl/operation/LoadAllOperation.java"),
//new ProjectFile(new HazelcastProject(), "hazelcast/src/main/java/com/hazelcast/map/impl/operation/GetOperation.java")
//new ProjectFile(new HazelcastProject(), "hazelcast/src/main/java/com/hazelcast/map/impl/operation/ReplaceIfSameOperation.java"),
//new ProjectFile(new HazelcastProject(), "hazelcast/src/main/java/com/hazelcast/map/impl/operation/AbstractMultipleEntryBackupOperation.java"),
//new ProjectFile(new HazelcastProject(), "hazelcast/src/main/java/com/hazelcast/map/impl/operation/MapReplicationStateHolder.java"),
//new ProjectFile(new HazelcastProject(), "hazelcast/src/main/java/com/hazelcast/map/impl/operation/EntryOperator.java"),
//new ProjectFile(new HazelcastProject(), "hazelcast/src/main/java/com/hazelcast/map/impl/operation/PutIfAbsentOperation.java"),
//new ProjectFile(new HazelcastProject(), "hazelcast/src/main/java/com/hazelcast/map/impl/operation/MapGetAllOperationFactory.java"),
//new ProjectFile(new HazelcastProject(), "hazelcast/src/main/java/com/hazelcast/map/impl/operation/PartitionWideEntryOperation.java"),
//new ProjectFile(new HazelcastProject(), "hazelcast/src/main/java/com/hazelcast/map/impl/operation/KeyLoadStatusOperation.java"),
//new ProjectFile(new HazelcastProject(), "hazelcast/src/main/java/com/hazelcast/map/impl/operation/DeleteOperation.java"),
//new ProjectFile(new HazelcastProject(), "hazelcast/src/main/java/com/hazelcast/map/impl/operation/SetTtlOperation.java"),
//new ProjectFile(new HazelcastProject(), "hazelcast/src/main/java/com/hazelcast/map/impl/operation/MergeOperationFactory.java"),
//new ProjectFile(new HazelcastProject(), "hazelcast/src/main/java/com/hazelcast/map/impl/operation/WanEventHolder.java"),
//new ProjectFile(new HazelcastProject(), "hazelcast/src/main/java/com/hazelcast/map/impl/operation/RemoveInterceptorOperation.java"),
//new ProjectFile(new HazelcastProject(), "hazelcast/src/main/java/com/hazelcast/map/impl/operation/MapFetchWithQueryOperation.java"),
//new ProjectFile(new HazelcastProject(), "hazelcast/src/main/java/com/hazelcast/map/impl/operation/GetEntryViewOperation.java"),
//new ProjectFile(new HazelcastProject(), "hazelcast/src/main/java/com/hazelcast/map/impl/operation/MapOperationProvider.java"),
//new ProjectFile(new HazelcastProject(), "hazelcast/src/main/java/com/hazelcast/map/impl/operation/PutBackupOperation.java"),
//new ProjectFile(new HazelcastProject(), "hazelcast/src/main/java/com/hazelcast/map/impl/operation/EvictBackupOperation.java"),
//new ProjectFile(new HazelcastProject(), "hazelcast/src/main/java/com/hazelcast/map/impl/operation/MapFlushOperation.java"),
//new ProjectFile(new HazelcastProject(), "hazelcast/src/main/java/com/hazelcast/map/impl/operation/MapGetInvalidationMetaDataOperation.java"),
//new ProjectFile(new HazelcastProject(), "hazelcast/src/main/java/com/hazelcast/map/impl/operation/MapSizeOperation.java"),
//new ProjectFile(new HazelcastProject(), "hazelcast/src/main/java/com/hazelcast/map/impl/operation/RemoveBackupOperation.java"),
//new ProjectFile(new HazelcastProject(), "hazelcast/src/main/java/com/hazelcast/map/impl/operation/MapFlushBackupOperation.java"),
//new ProjectFile(new HazelcastProject(), "hazelcast/src/main/java/com/hazelcast/map/impl/operation/IsPartitionLoadedOperationFactory.java"),
//new ProjectFile(new HazelcastProject(), "hazelcast/src/main/java/com/hazelcast/map/impl/operation/ClearOperation.java"),
//new ProjectFile(new HazelcastProject(), "hazelcast/src/main/java/com/hazelcast/map/impl/operation/PutOperation.java"),
//new ProjectFile(new HazelcastProject(), "hazelcast/src/main/java/com/hazelcast/map/impl/operation/PartitionWideEntryWithPredicateOperation.java"),
//new ProjectFile(new HazelcastProject(), "hazelcast/src/main/java/com/hazelcast/map/impl/operation/package-info.java"),
//new ProjectFile(new HazelcastProject(), "hazelcast/src/main/java/com/hazelcast/map/impl/operation/ClearBackupOperation.java"),
//new ProjectFile(new HazelcastProject(), "hazelcast/src/main/java/com/hazelcast/map/impl/operation/PartitionWideEntryBackupOperation.java"),
//new ProjectFile(new HazelcastProject(), "hazelcast/src/main/java/com/hazelcast/map/impl/operation/NotifyMapFlushOperation.java"),
//new ProjectFile(new HazelcastProject(), "hazelcast/src/main/java/com/hazelcast/map/impl/operation/SetOperation.java"),
//new ProjectFile(new HazelcastProject(), "hazelcast/src/main/java/com/hazelcast/map/impl/operation/RemoveIfSameOperation.java"),
//new ProjectFile(new HazelcastProject(), "hazelcast/src/main/java/com/hazelcast/map/impl/operation/DefaultMapOperationProvider.java"),
//new ProjectFile(new HazelcastProject(), "hazelcast/src/main/java/com/hazelcast/map/impl/operation/MergeOperation.java"),
//new ProjectFile(new HazelcastProject(), "hazelcast/src/main/java/com/hazelcast/map/impl/operation/PutTransientOperation.java"),
//new ProjectFile(new HazelcastProject(), "hazelcast/src/main/java/com/hazelcast/map/impl/operation/PutFromLoadAllBackupOperation.java"),
//new ProjectFile(new HazelcastProject(), "hazelcast/src/main/java/com/hazelcast/map/impl/operation/PutAllOperation.java"),
//new ProjectFile(new HazelcastProject(), "hazelcast/src/main/java/com/hazelcast/map/impl/operation/EntryBackupOperation.java"),
//new ProjectFile(new HazelcastProject(), "hazelcast/src/main/java/com/hazelcast/map/impl/operation/TriggerLoadIfNeededOperation.java"),
//new ProjectFile(new HazelcastProject(), "hazelcast/src/main/java/com/hazelcast/map/impl/operation/ClearNearCacheOperation.java"),
//new ProjectFile(new HazelcastProject(), "hazelcast/src/main/java/com/hazelcast/map/impl/InternalMapPartitionLostListenerAdapter.java")
//                ),
//                Splitter.on(' ').splitToList("-classpath /home/monty/IdeaProjects/java-corpus/hazelcast/hazelcast/target/classes:/home/monty/.m2/repository/com/fasterxml/jackson/core/jackson-core/2.9.7/jackson-core-2.9.7.jar:/home/monty/.m2/repository/log4j/log4j/1.2.17/log4j-1.2.17.jar:/home/monty/.m2/repository/org/apache/logging/log4j/log4j-api/2.3/log4j-api-2.3.jar:/home/monty/.m2/repository/org/apache/logging/log4j/log4j-core/2.3/log4j-core-2.3.jar:/home/monty/.m2/repository/org/slf4j/slf4j-api/1.7.25/slf4j-api-1.7.25.jar:/home/monty/.m2/repository/com/hazelcast/hazelcast-client-protocol/1.8.0-1/hazelcast-client-protocol-1.8.0-1.jar:/home/monty/.m2/repository/org/osgi/org.osgi.core/4.2.0/org.osgi.core-4.2.0.jar:/home/monty/.m2/repository/org/codehaus/groovy/groovy-all/2.1.8/groovy-all-2.1.8.jar:/home/monty/.m2/repository/org/jruby/jruby-complete/1.7.22/jruby-complete-1.7.22.jar:/home/monty/.m2/repository/javax/cache/cache-api/1.1.0/cache-api-1.1.0.jar:/home/monty/.m2/repository/org/snakeyaml/snakeyaml-engine/1.0/snakeyaml-engine-1.0.jar:/home/monty/.m2/repository/com/google/code/findbugs/annotations/3.0.0/annotations-3.0.0.jar: -sourcepath /home/monty/IdeaProjects/java-corpus/hazelcast/hazelcast/src/main/java: -s /home/monty/IdeaProjects/java-corpus/hazelcast/hazelcast/target/generated-sources/annotations -g -encoding UTF-8"));

//        Collection<Diagnostic<? extends JavaFileObject>> diagnostics =
//                DiagnosticsCollector.collectDiagnosticsFromScans(ImmutableList.of(scan), Verbosity.VERBOSE);
//
//        diagnostics.forEach(System.out::println);

//        System.out.println(Iterables.size(Iterables.filter(diagnostics,
//                diag -> diag.getSource().getName().contains("map/impl/operation/GetOperation.java"))));

        new ProjectHarness(new HazelcastProject(), Verbosity.VERBOSE)
                .serialiseCommit(GitUtils.parseCommit(project.loadRepo(), "24393f7a05ec7304e744d62a90c781156e37d7e3"),
                        Paths.get("/home/monty/IdeaProjects/java-corpus/foo_200"));

//        oldCommit = "c8f6e190252beec44760fac401b61ebe90388f34";
//        new ProjectHarness(project, Verbosity.VERBOSE)
//                .serialiseCommit(GitUtils.parseCommit(project.loadRepo(), oldCommit),
//                        Paths.get("/rds/user/am2857/hpc-work/diagnostics/hazelcast500/16 c8f6e190252beec44760fac401b61ebe90388f34.2"));
    }

    @Test
    public void test2() {
        DiagnosticsScan scan = new DiagnosticsScan("default-compile",
                ImmutableList.of(
new ProjectFile(new HazelcastProject(), "hazelcast/src/main/java/com/hazelcast/internal/partition/PartitionServiceProxy.java"),
new ProjectFile(new HazelcastProject(), "hazelcast/src/main/java/com/hazelcast/internal/metrics/MetricsUtil.java")
                ),
                Splitter.on(' ').splitToList("-classpath /home/monty/IdeaProjects/java-corpus/hazelcast/hazelcast/target/classes:/home/monty/.m2/repository/com/fasterxml/jackson/core/jackson-core/2.9.7/jackson-core-2.9.7.jar:/home/monty/.m2/repository/log4j/log4j/1.2.17/log4j-1.2.17.jar:/home/monty/.m2/repository/org/apache/logging/log4j/log4j-api/2.3/log4j-api-2.3.jar:/home/monty/.m2/repository/org/apache/logging/log4j/log4j-core/2.3/log4j-core-2.3.jar:/home/monty/.m2/repository/org/slf4j/slf4j-api/1.7.25/slf4j-api-1.7.25.jar:/home/monty/.m2/repository/com/hazelcast/hazelcast-client-protocol/1.8.0-1/hazelcast-client-protocol-1.8.0-1.jar:/home/monty/.m2/repository/org/osgi/org.osgi.core/4.2.0/org.osgi.core-4.2.0.jar:/home/monty/.m2/repository/org/codehaus/groovy/groovy-all/2.1.8/groovy-all-2.1.8.jar:/home/monty/.m2/repository/org/jruby/jruby-complete/1.7.22/jruby-complete-1.7.22.jar:/home/monty/.m2/repository/javax/cache/cache-api/1.1.0/cache-api-1.1.0.jar:/home/monty/.m2/repository/org/snakeyaml/snakeyaml-engine/1.0/snakeyaml-engine-1.0.jar:/home/monty/.m2/repository/com/google/code/findbugs/annotations/3.0.0/annotations-3.0.0.jar: -sourcepath /home/monty/IdeaProjects/java-corpus/hazelcast/hazelcast/src/main/java: -s /home/monty/IdeaProjects/java-corpus/hazelcast/hazelcast/target/generated-sources/annotations -g -encoding UTF-8 -parameters -source 1.8 -target 1.8"));

        Collection<Diagnostic<? extends JavaFileObject>> diagnostics = DiagnosticsCollector.collectDiagnosticsFromScans(ImmutableList.of(scan), Verbosity.VERBOSE);

//        diagnostics.stream().filter(diag -> diag.getSource().getName().contains("MetricsUtil.java")).forEach(System.out::println);
        diagnostics.stream().forEach(System.out::println);

//        diagnostics.forEach(System.out::println);
    }
}
