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

package tachyon.master.file.meta;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.google.common.collect.Lists;

import tachyon.Constants;
import tachyon.exception.FileAlreadyCompletedException;
import tachyon.exception.InvalidFileSizeException;
import tachyon.exception.BlockInfoException;

/**
 * Unit tests for {@link InodeFile}.
 */
public final class InodeFileTest extends AbstractInodeTest {
  private static final long LENGTH = 100;

  /**
   * Tests the {@link InodeFile#equals(Object)} method.
   */
  @Test
  public void equalsTest() {
    InodeFile inode1 = createInodeFile(1);
    // self equal
    Assert.assertEquals(inode1, inode1);
    InodeFile inode2 = createInodeFile(1);
    // equal with same id
    Assert.assertEquals(inode1, inode2);
    InodeFile inode3 = createInodeFile(3);
    Assert.assertFalse(inode1.equals(inode3));
  }

  /**
   * Tests the {@link InodeFile#getId()} method.
   */
  @Test
  public void getIdTest() {
    InodeFile inode1 = createInodeFile(1);
    Assert.assertEquals(createInodeFileId(1), inode1.getId());
  }

  /**
   * Tests the {@link InodeFile#setLength(long)} method.
   */
  @Test
  public void setLengthTest() {
    InodeFile inodeFile = createInodeFile(1);
    inodeFile.setLength(LENGTH);
    Assert.assertEquals(LENGTH, inodeFile.getLength());
  }

  /**
   * Tests that an exception is thrown when trying to create a file with a negative length.
   *
   * @throws Exception if completing the file fails
   */
  @Test
  public void setNegativeLengthTest() throws Exception {
    mThrown.expect(InvalidFileSizeException.class);
    mThrown.expectMessage("File testFile1 cannot have negative length.");
    InodeFile inodeFile = createInodeFile(1);
    inodeFile.complete(-1);
  }

  /**
   * Tests that an exception is thrown when trying to complete a file twice.
   *
   * @throws Exception if completing the file fails
   */
  @Test
  public void completeTwiceTest() throws Exception {
    mThrown.expect(FileAlreadyCompletedException.class);
    mThrown.expectMessage("File testFile1 has already been completed.");
    InodeFile inodeFile = createInodeFile(1);
    inodeFile.complete(LENGTH);
    inodeFile.complete(LENGTH);
  }

  /**
   * Tests the {@link InodeFile#getBlockSizeBytes()} method.
   */
  @Test
  public void getBlockSizeBytesTest() {
    InodeFile inode1 = createInodeFile(1);
    Assert.assertEquals(Constants.KB, inode1.getBlockSizeBytes());
  }

  /**
   * Tests the {@link InodeFile#getBlockIdByIndex(int)} method.
   *
   * @throws Exception if getting the block id by its index fails
   */
  @Test
  public void getBlockIdByIndexTest() throws Exception {
    InodeFile inodeFile = createInodeFile(1);
    List<Long> blockIds = Lists.newArrayList();
    final int NUM_BLOCKS = 3;
    for (int i = 0; i < NUM_BLOCKS; i ++) {
      blockIds.add(inodeFile.getNewBlockId());
    }
    for (int i = 0; i < NUM_BLOCKS; i ++) {
      Assert.assertEquals(blockIds.get(i), (Long) inodeFile.getBlockIdByIndex(i));
    }
    try {
      inodeFile.getBlockIdByIndex(-1);
      Assert.fail();
    } catch (BlockInfoException e) {
      Assert.assertEquals(String.format("blockIndex -1 is out of range. File blocks: %d",
          NUM_BLOCKS), e.getMessage());
    }
    try {
      inodeFile.getBlockIdByIndex(NUM_BLOCKS);
      Assert.fail();
    } catch (BlockInfoException e) {
      Assert.assertEquals(String.format("blockIndex %d is out of range. File blocks: %d",
          NUM_BLOCKS, NUM_BLOCKS), e.getMessage());
    }
  }

  /**
   * Tests the {@link InodeFile#setCompleted(boolean)} method.
   */
  @Test
  public void setCompletedTest() {
    InodeFile inode1 = createInodeFile(1);
    Assert.assertFalse(inode1.isCompleted());

    inode1.setCompleted(true);
    Assert.assertTrue(inode1.isCompleted());
  }

  /**
   * Tests the {@link InodeFile#getPermission()} method.
   */
  @Test
  public void permissionStatusTest() {
    InodeFile inode1 = createInodeFile(1);
    Assert.assertEquals(AbstractInodeTest.TEST_USER_NAME, inode1.getUserName());
    Assert.assertEquals(AbstractInodeTest.TEST_GROUP_NAME, inode1.getGroupName());
    Assert.assertEquals((short)0644, inode1.getPermission());
  }
}
