/*
 * Licensed to the University of California, Berkeley under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package tachyon.master.lineage;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.internal.util.reflection.Whitebox;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.google.common.collect.Lists;

import tachyon.TachyonURI;
import tachyon.exception.ExceptionMessage;
import tachyon.exception.InvalidPathException;
import tachyon.exception.LineageDeletionException;
import tachyon.exception.LineageDoesNotExistException;
import tachyon.job.CommandLineJob;
import tachyon.job.Job;
import tachyon.job.JobConf;
import tachyon.master.file.FileSystemMaster;
import tachyon.master.file.options.CompleteFileOptions;
import tachyon.master.journal.Journal;
import tachyon.master.journal.ReadWriteJournal;
import tachyon.wire.FileInfo;
import tachyon.wire.LineageInfo;

/**
 * Unit tests for {@link LineageMaster}.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({FileSystemMaster.class})
public final class LineageMasterTest {
  private LineageMaster mLineageMaster;
  private FileSystemMaster mFileSystemMaster;
  private Job mJob;

  /** Rule to create a new temporary folder during each test. */
  @Rule
  public TemporaryFolder mTestFolder = new TemporaryFolder();

  /**
   * Sets up all dependencies before a test runs.
   *
   * @throws Exception if setting up the test fails
   */
  @Before
  public void before() throws Exception {
    Journal journal = new ReadWriteJournal(mTestFolder.newFolder().getAbsolutePath());
    mFileSystemMaster = Mockito.mock(FileSystemMaster.class);
    mLineageMaster = new LineageMaster(mFileSystemMaster, journal);
    mLineageMaster.start(true);
    mJob = new CommandLineJob("test", new JobConf("output"));
  }

  /**
   * Tests the {@link LineageMaster#getLineageInfoList()} method.
   *
   * @throws Exception if an operation on a master fails
   */
  @Test
  public void listLineagesTest() throws Exception {
    mLineageMaster.createLineage(Lists.<TachyonURI>newArrayList(),
        Lists.newArrayList(new TachyonURI("/test1")), mJob);
    mLineageMaster.createLineage(Lists.newArrayList(new TachyonURI("/test1")),
        Lists.newArrayList(new TachyonURI("/test2")), mJob);
    Mockito.when(mFileSystemMaster.getPath(Mockito.anyLong())).thenReturn(new TachyonURI("test"));
    List<LineageInfo> info = mLineageMaster.getLineageInfoList();
    Assert.assertEquals(2, info.size());
  }

  /**
   * Tests that an exception is thrown when trying to create a lineage for a non-existing file via
   * the {@link LineageMaster#createLineage(List, List, Job)} method.
   *
   * @throws Exception if an operation on a master fails
   */
  @Test
  public void createLineageWithNonExistingFileTest() throws Exception {
    TachyonURI missingInput = new TachyonURI("/test1");
    Mockito.when(mFileSystemMaster.getFileId(missingInput)).thenReturn(-1L);
    // try catch block used because ExpectedExceptionRule conflicts with Powermock
    try {
      mLineageMaster.createLineage(Lists.newArrayList(missingInput),
          Lists.newArrayList(new TachyonURI("/test2")), mJob);
      Assert.fail();
    } catch (InvalidPathException e) {
      Assert.assertEquals(ExceptionMessage.LINEAGE_INPUT_FILE_NOT_EXIST.getMessage("/test1"),
          e.getMessage());
    }
  }

  /**
   * Tests the {@link LineageMaster#deleteLineage(long, boolean)} method.
   *
   * @throws Exception if an operation on a master fails
   */
  @Test
  public void deleteLineageTest() throws Exception {
    long l1 = mLineageMaster.createLineage(Lists.<TachyonURI>newArrayList(),
        Lists.newArrayList(new TachyonURI("/test1")), mJob);
    mLineageMaster.createLineage(Lists.newArrayList(new TachyonURI("/test1")),
        Lists.newArrayList(new TachyonURI("/test2")), mJob);
    mLineageMaster.deleteLineage(l1, true);
    List<LineageInfo> info = mLineageMaster.getLineageInfoList();
    Assert.assertEquals(0, info.size());
  }

  /**
   * Tests that an exception is thrown when trying to delete a non-existing lineage via the
   * {@link LineageMaster#deleteLineage(long, boolean)} method.
   *
   * @throws Exception if an operation on a master fails
   */
  @Test
  public void deleteNonexistingLineageTest() throws Exception {
    long id = 1L;
    try {
      mLineageMaster.deleteLineage(id, false);
      Assert.fail();
    } catch (LineageDoesNotExistException e) {
      Assert.assertEquals(ExceptionMessage.LINEAGE_DOES_NOT_EXIST.getMessage(id), e.getMessage());
    }
  }

  /**
   * Tests that an exception is thrown when trying to delete a lineage with children via the
   * {@link LineageMaster#deleteLineage(long, boolean)} without setting the {@code cascade} flag to
   * {@code true}.
   *
   * @throws Exception if an operation on a master fails
   */
  @Test
  public void deleteLineageWithChildrenTest() throws Exception {
    long l1 = mLineageMaster.createLineage(Lists.<TachyonURI>newArrayList(),
        Lists.newArrayList(new TachyonURI("/test1")), mJob);
    mLineageMaster.createLineage(Lists.newArrayList(new TachyonURI("/test1")),
        Lists.newArrayList(new TachyonURI("/test2")), mJob);
    try {
      mLineageMaster.deleteLineage(l1, false);
      Assert.fail();
    } catch (LineageDeletionException e) {
      Assert.assertEquals(ExceptionMessage.DELETE_LINEAGE_WITH_CHILDREN.getMessage(l1),
          e.getMessage());
    }
  }

  /**
   * Tests the {@link LineageMaster#reinitializeFile(String, long, long)} method.
   *
   * @throws Exception if an operation on a master fails
   */
  @Test
  public void reinitializeFileTest() throws Exception {
    mLineageMaster.createLineage(Lists.<TachyonURI>newArrayList(),
        Lists.newArrayList(new TachyonURI("/test1")), mJob);
    FileInfo fileInfo = new FileInfo();
    fileInfo.setCompleted(false);
    Mockito.when(mFileSystemMaster.getFileInfo(Mockito.any(Long.class))).thenReturn(fileInfo);
    mLineageMaster.reinitializeFile("/test1", 500L, 10L);
    Mockito.verify(mFileSystemMaster).reinitializeFile(new TachyonURI("/test1"), 500L, 10L);
  }

  /**
   * Tests that completing a file asynchronously works.
   *
   * @throws Exception if an operation on a master fails
   */
  @Test
  public void asyncCompleteFileTest() throws Exception {
    TachyonURI file = new TachyonURI("/test1");
    mLineageMaster.createLineage(Lists.<TachyonURI>newArrayList(), Lists.newArrayList(file), mJob);
    mFileSystemMaster.completeFile(file, CompleteFileOptions.defaults());
    Mockito.verify(mFileSystemMaster).completeFile(Mockito.eq(file),
        Mockito.any(CompleteFileOptions.class));
  }

  /**
   * Tests the {@link LineageMaster#stop()} method.
   *
   * @throws Exception if stopping the master fails
   */
  @Test
  public void stopTest() throws Exception {
    ExecutorService service =
        (ExecutorService) Whitebox.getInternalState(mLineageMaster, "mExecutorService");
    Future<?> checkpointThread =
        (Future<?>) Whitebox.getInternalState(mLineageMaster, "mCheckpointExecutionService");
    Future<?> recomputeThread =
        (Future<?>) Whitebox.getInternalState(mLineageMaster, "mRecomputeExecutionService");
    Assert.assertFalse(checkpointThread.isDone());
    Assert.assertFalse(recomputeThread.isDone());
    Assert.assertFalse(service.isShutdown());
    mLineageMaster.stop();
    Assert.assertTrue(checkpointThread.isDone());
    Assert.assertTrue(recomputeThread.isDone());
    Assert.assertTrue(service.isShutdown());
  }
}
