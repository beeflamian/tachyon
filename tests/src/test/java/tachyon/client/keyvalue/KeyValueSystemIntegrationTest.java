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

package tachyon.client.keyvalue;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.google.common.collect.Lists;

import tachyon.Constants;
import tachyon.LocalTachyonClusterResource;
import tachyon.TachyonURI;
import tachyon.client.ClientContext;
import tachyon.client.file.FileSystem;
import tachyon.client.file.URIStatus;
import tachyon.exception.ExceptionMessage;
import tachyon.exception.TachyonException;
import tachyon.util.io.BufferUtils;
import tachyon.util.io.PathUtils;

/**
 * Integration tests for {@link KeyValueSystem}.
 */
public final class KeyValueSystemIntegrationTest {
  private static final int BLOCK_SIZE = 512 * Constants.MB;
  private static final String BASE_KEY = "base_key";
  private static final String BASE_VALUE = "base_value";
  private static final int BASE_KEY_VALUE_NUMBER = 100;
  private static final byte[] KEY1 = "key1".getBytes();
  private static final byte[] KEY2 = "key2_foo".getBytes();
  private static final byte[] VALUE1 = "value1".getBytes();
  private static final byte[] VALUE2 = "value2_bar".getBytes();
  private static KeyValueSystem sKeyValueSystem;

  private KeyValueStoreWriter mWriter;
  private KeyValueStoreReader mReader;
  private TachyonURI mStoreUri;

  @Rule
  public final ExpectedException mThrown = ExpectedException.none();

  @ClassRule
  public static LocalTachyonClusterResource sLocalTachyonClusterResource =
      new LocalTachyonClusterResource(Constants.GB, BLOCK_SIZE,
          /* ensure key-value service is turned on */
          Constants.KEY_VALUE_ENABLED, "true");

  @BeforeClass
  public static void beforeClass() throws Exception {
    sKeyValueSystem = KeyValueSystem.Factory.create();
  }

  @Before
  public void before() throws Exception {
    mStoreUri = new TachyonURI(PathUtils.uniqPath());
  }

  /**
   * Tests creating and opening an empty store.
   */
  @Test
  public void createAndOpenEmptyStoreTest() throws Exception {
    mWriter = sKeyValueSystem.createStore(mStoreUri);
    Assert.assertNotNull(mWriter);
    mWriter.close();

    mReader = sKeyValueSystem.openStore(mStoreUri);
    Assert.assertNotNull(mReader);
    mReader.close();
  }

  /**
   * Tests creating and opening a store with one key.
   */
  @Test
  public void createAndOpenStoreWithOneKeyTest() throws Exception {
    mWriter = sKeyValueSystem.createStore(mStoreUri);
    mWriter.put(KEY1, VALUE1);
    mWriter.close();

    mReader = sKeyValueSystem.openStore(mStoreUri);
    Assert.assertArrayEquals(VALUE1, mReader.get(KEY1));
    Assert.assertNull(mReader.get(KEY2));
    mReader.close();
  }

  /**
   * Tests creating and opening a store with a number of key.
   */
  @Test
  public void createAndOpenStoreWithMultiKeysTest() throws Exception {
    final int numKeys = 100;
    final int keyLength = 4; // 4Byte key
    final int valueLength = 5 * Constants.KB; // 5KB value
    mWriter = sKeyValueSystem.createStore(mStoreUri);
    for (int i = 0; i < numKeys; i ++) {
      byte[] key = BufferUtils.getIncreasingByteArray(i, keyLength);
      byte[] value = BufferUtils.getIncreasingByteArray(i, valueLength);
      mWriter.put(key, value);
    }
    mWriter.close();

    mReader = sKeyValueSystem.openStore(mStoreUri);
    for (int i = 0; i < numKeys; i ++) {
      byte[] key = BufferUtils.getIncreasingByteArray(i, keyLength);
      byte[] value = mReader.get(key);
      Assert.assertTrue(BufferUtils.equalIncreasingByteArray(i, valueLength, value));
    }
    Assert.assertNull(mReader.get(KEY1));
    Assert.assertNull(mReader.get(KEY2));
    mReader.close();
  }

  /**
   * Tests that an iterator for an empty store has no next elements.
   */
  @Test
  public void emptyStoreIteratorTest() throws Exception {
    mWriter = sKeyValueSystem.createStore(mStoreUri);
    mWriter.close();

    mReader = sKeyValueSystem.openStore(mStoreUri);
    KeyValueIterator iterator = mReader.iterator();
    Assert.assertFalse(iterator.hasNext());
  }

  /**
   * Generates a key in the format {@link #BASE_KEY}_{@code id}.
   *
   * @param id the id of the key
   * @return the generated key
   */
  private String genBaseKey(int id) {
    return String.format("%s_%d", BASE_KEY, id);
  }

  /**
   * Generates a value in the format {@link #BASE_VALUE}_{@code id}.
   *
   * @param id the id of the value
   * @return the generated value
   */
  private String genBaseValue(int id) {
    return String.format("%s_%d", BASE_VALUE, id);
  }

  /**
   * Tests that an iterator can correctly iterate over a store.
   * <p>
   * There is no assumption about the order of iteration, it just makes sure all key-value pairs are
   * iterated.
   */
  @Test
  public void noOrderIteratorTest() throws Exception {
    List<TachyonURI> storeUris = Lists.newArrayList();
    List<List<KeyValuePair>> keyValuePairs = Lists.newArrayList();

    List<KeyValuePair> pairs = Lists.newArrayList();
    storeUris.add(createStoreOfSize(0, pairs));
    keyValuePairs.add(pairs);

    pairs = Lists.newArrayList();
    storeUris.add(createStoreOfSize(2, pairs));
    keyValuePairs.add(pairs);

    pairs = Lists.newArrayList();
    storeUris.add(createStoreOfMultiplePartitions(3,pairs));
    keyValuePairs.add(pairs);

    int numStoreUri = storeUris.size();
    for (int i = 0; i < numStoreUri; i ++) {
      List<KeyValuePair> expectedPairs = keyValuePairs.get(i);
      List<KeyValuePair> iteratedPairs = Lists.newArrayList();
      mReader = sKeyValueSystem.openStore(storeUris.get(i));
      KeyValueIterator iterator = mReader.iterator();
      while (iterator.hasNext()) {
        iteratedPairs.add(iterator.next());
      }

      // If size is not the same, no need for the time-consuming list comparison below.
      Assert.assertEquals(expectedPairs.size(), iteratedPairs.size());
      // Sorts and then compares pairs and iteratedPairs.
      Collections.sort(expectedPairs);
      Collections.sort(iteratedPairs);
      Assert.assertEquals(expectedPairs, iteratedPairs);
    }
  }

  /**
   * Tests creating and opening a store with a number of keys, while each key-value pair is large
   * enough to take a separate key-value partition.
   */
  @Test
  public void createMultiPartitionsTest() throws Exception {
    // TODO(cc): Remove codes using createStoreOfMultiplePartitions.
    final long maxPartitionSize = Constants.MB; // Each partition is at most 1 MB
    final int numKeys = 10;
    final int keyLength = 4; // 4Byte key
    final int valueLength = 500 * Constants.KB; // 500KB value

    FileSystem fs = FileSystem.Factory.get();

    ClientContext.getConf().set(Constants.KEY_VALUE_PARTITION_SIZE_BYTES_MAX,
        String.valueOf(maxPartitionSize));
    mWriter = sKeyValueSystem.createStore(mStoreUri);
    for (int i = 0; i < numKeys; i ++) {
      byte[] key = BufferUtils.getIncreasingByteArray(i, keyLength);
      byte[] value = BufferUtils.getIncreasingByteArray(i, valueLength);
      mWriter.put(key, value);
    }
    mWriter.close();

    List<URIStatus> files = fs.listStatus(mStoreUri);
    Assert.assertEquals(numKeys, files.size());
    for (URIStatus info : files) {
      Assert.assertTrue(info.getLength() <= maxPartitionSize);
    }

    mReader = sKeyValueSystem.openStore(mStoreUri);
    for (int i = 0; i < numKeys; i ++) {
      byte[] key = BufferUtils.getIncreasingByteArray(i, keyLength);
      byte[] value = mReader.get(key);
      Assert.assertTrue(BufferUtils.equalIncreasingByteArray(i, valueLength, value));
    }
    Assert.assertNull(mReader.get(KEY1));
    Assert.assertNull(mReader.get(KEY2));
    mReader.close();
  }

  /**
   * Tests putting a key-value pair that is larger than the max key-value partition size,
   * expecting exception thrown.
   */
  @Test
  public void putKeyValueTooLargeTest() throws Exception {
    final long maxPartitionSize = 500 * Constants.KB; // Each partition is at most 500 KB
    final int keyLength = 4; // 4Byte key
    final int valueLength = 500 * Constants.KB; // 500KB value

    ClientContext.getConf().set(Constants.KEY_VALUE_PARTITION_SIZE_BYTES_MAX,
        String.valueOf(maxPartitionSize));
    mWriter = sKeyValueSystem.createStore(mStoreUri);
    byte[] key = BufferUtils.getIncreasingByteArray(0, keyLength);
    byte[] value = BufferUtils.getIncreasingByteArray(0, valueLength);

    mThrown.expect(IOException.class);
    mThrown.expectMessage(ExceptionMessage.KEY_VALUE_TOO_LARGE.getMessage(keyLength, valueLength));
    mWriter.put(key, value);
  }

  /**
   * Creates a store with the specified number of key-value pairs. The key-value pairs are in the
   * format specified in {@link #genBaseKey(int)} and {@link #genBaseValue(int)} with id starts
   * from 0.
   *
   * The created store's size is {@link Assert}ed before return.
   *
   * @param size the number of key-value pairs
   * @param pairs the key-value pairs in the store, null if you don't want to know them
   * @return the URI to the store
   * @throws Exception if any error happens
   */
  private TachyonURI createStoreOfSize(int size, List<KeyValuePair> pairs) throws Exception {
    TachyonURI path = new TachyonURI(PathUtils.uniqPath());
    KeyValueStoreWriter writer = sKeyValueSystem.createStore(path);
    for (int i = 0; i < size; i ++) {
      byte[] key = genBaseKey(i).getBytes();
      byte[] value = genBaseValue(i).getBytes();
      writer.put(key, value);
      if (pairs != null) {
        pairs.add(new KeyValuePair(key, value));
      }
    }
    writer.close();

    Assert.assertEquals(size, sKeyValueSystem.openStore(path).size());

    return path;
  }

  private int getPartitionNumber(TachyonURI storeUri) throws Exception {
    KeyValueMasterClient client =
        new KeyValueMasterClient(ClientContext.getMasterAddress(), ClientContext.getConf());
    return client.getPartitionInfo(storeUri).size();
  }

  /**
   * Creates a store with the specified number of partitions.
   *
   * NOTE: calling this method will set {@link Constants#KEY_VALUE_PARTITION_SIZE_BYTES_MAX} to
   * {@link Constants#MB}.
   *
   * @param partitionNumber the number of partitions
   * @param keyValuePairs the key-value pairs in the store, null if you don't want to know them
   * @return the URI to the created store
   */
  private TachyonURI createStoreOfMultiplePartitions(int partitionNumber,
      List<KeyValuePair> keyValuePairs) throws Exception {
    // These sizes are carefully selected, one partition holds only one key-value pair.
    final long maxPartitionSize = Constants.MB; // Each partition is at most 1 MB
    ClientContext.getConf().set(Constants.KEY_VALUE_PARTITION_SIZE_BYTES_MAX,
        String.valueOf(maxPartitionSize));
    final int keyLength = 4; // 4Byte key
    final int valueLength = 500 * Constants.KB; // 500KB value

    TachyonURI storeUri = new TachyonURI(PathUtils.uniqPath());
    mWriter = sKeyValueSystem.createStore(storeUri);
    for (int i = 0; i < partitionNumber; i ++) {
      byte[] key = BufferUtils.getIncreasingByteArray(i, keyLength);
      byte[] value = BufferUtils.getIncreasingByteArray(i, valueLength);
      mWriter.put(key, value);
      if (keyValuePairs != null) {
        keyValuePairs.add(new KeyValuePair(key, value));
      }
    }
    mWriter.close();

    Assert.assertEquals(partitionNumber, getPartitionNumber(storeUri));

    return storeUri;
  }

  private void testDeleteStore(TachyonURI storeUri) throws Exception {
    sKeyValueSystem.deleteStore(storeUri);

    // TachyonException is expected to be thrown.
    try {
      sKeyValueSystem.openStore(storeUri);
    } catch (TachyonException e) {
      Assert.assertEquals(e.getMessage(),
          ExceptionMessage.PATH_DOES_NOT_EXIST.getMessage(storeUri));
      return;
    }
    Assert.assertTrue("The URI to the deleted key-value store still exists", false);
  }

  /**
   * Tests that a store of various sizes (including empty store) can be correctly deleted.
   */
  @Test
  public void deleteStoreTest() throws Exception {
    List<TachyonURI> storeUris = Lists.newArrayList();
    storeUris.add(createStoreOfSize(0, null));
    storeUris.add(createStoreOfSize(2, null));
    storeUris.add(createStoreOfMultiplePartitions(3, null));

    for (TachyonURI storeUri : storeUris) {
      testDeleteStore(storeUri);
    }
  }

  private void testMergeStore(TachyonURI store1, List<KeyValuePair> keyValuePairs1,
      TachyonURI store2, List<KeyValuePair> keyValuePairs2) throws Exception {
    sKeyValueSystem.mergeStore(store1, store2);

    // store2 contains all key-value pairs in both store1 and store2.
    List<KeyValuePair> mergedPairs = Lists.newArrayList();
    mergedPairs.addAll(keyValuePairs1);
    mergedPairs.addAll(keyValuePairs2);

    List<KeyValuePair> store2Pairs = Lists.newArrayList();
    KeyValueIterator iterator = sKeyValueSystem.openStore(store2).iterator();
    while (iterator.hasNext()) {
      store2Pairs.add(iterator.next());
    }

    // If size is not the same, no need for the time-consuming list comparison below.
    Assert.assertEquals(mergedPairs.size(), store2Pairs.size());
    Collections.sort(mergedPairs);
    Collections.sort(store2Pairs);
    Assert.assertEquals(mergedPairs, store2Pairs);

    // store1 no longer exists, because it has been merged into store2.
    // TachyonException is expected to be thrown.
    try {
      sKeyValueSystem.openStore(store1);
    } catch (TachyonException e) {
      Assert.assertEquals(e.getMessage(),
          ExceptionMessage.PATH_DOES_NOT_EXIST.getMessage(store1));
      return;
    }
    Assert.assertTrue("The URI to the deleted key-value store still exists", false);
  }

  /**
   * Tests that two stores of various sizes (including empty store) can be correctly merged.
   */
  @Test
  public void mergeStoreTest() throws Exception {
    final int storeOfSize = 1;
    final int storeOfPartitions = 2;
    final int[][] storeCreationMethodAndParameter = new int[][]{
        {storeOfSize, 0},
        {storeOfSize, 2},
        {storeOfPartitions, 3}
    };
    final int length = storeCreationMethodAndParameter.length;

    for (int i = 0; i < length; i ++) {
      for (int j = 0; j < length; j ++) {
        int method1 = storeCreationMethodAndParameter[i][0];
        int parameter1 = storeCreationMethodAndParameter[i][1];
        List<KeyValuePair> pairs1 = Lists.newArrayList();
        TachyonURI storeUri1 = method1 == storeOfSize ? createStoreOfSize(parameter1, pairs1) :
            createStoreOfMultiplePartitions(parameter1, pairs1);

        int method2 = storeCreationMethodAndParameter[j][0];
        int parameter2 = storeCreationMethodAndParameter[j][1];
        List<KeyValuePair> pairs2 = Lists.newArrayList();
        TachyonURI storeUri2 = method2 == storeOfSize ? createStoreOfSize(parameter2, pairs2) :
            createStoreOfMultiplePartitions(parameter2, pairs2);

        testMergeStore(storeUri1, pairs1, storeUri2, pairs2);
      }
    }
  }
}
