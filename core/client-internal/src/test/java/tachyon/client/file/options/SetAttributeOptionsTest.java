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

package tachyon.client.file.options;

import java.util.Random;

import org.junit.Assert;
import org.junit.Test;

/**
 * Tests for the {@link SetAttributeOptions} class.
 */
public class SetAttributeOptionsTest {
  @Test
  public void defaultsTest() {
    SetAttributeOptions options = SetAttributeOptions.defaults();
    Assert.assertFalse(options.hasPersisted());
    Assert.assertFalse(options.hasPinned());
    Assert.assertFalse(options.hasTtl());
    Assert.assertFalse(options.hasOwner());
    Assert.assertFalse(options.hasGroup());
    Assert.assertFalse(options.hasPermission());
    Assert.assertFalse(options.hasRecursive());
  }

  /**
   * Tests getting and setting fields.
   */
  @Test
  public void fieldsTest() {
    Random random = new Random();
    boolean persisted = random.nextBoolean();
    boolean pinned = random.nextBoolean();
    long ttl = random.nextLong();
    byte[] bytes = new byte[5];
    random.nextBytes(bytes);
    String owner = new String(bytes);
    random.nextBytes(bytes);
    String group = new String(bytes);
    short permission = (short) random.nextInt();
    boolean recursive = random.nextBoolean();

    SetAttributeOptions options = SetAttributeOptions.defaults();
    options.setPersisted(persisted);
    options.setPinned(pinned);
    options.setTtl(ttl);
    options.setOwner(owner);
    options.setGroup(group);
    options.setPermission(permission);
    options.setRecursive(recursive);

    Assert.assertTrue(options.hasPersisted());
    Assert.assertEquals(persisted, options.getPersisted());
    Assert.assertTrue(options.hasPinned());
    Assert.assertEquals(pinned, options.getPinned());
    Assert.assertTrue(options.hasTtl());
    Assert.assertEquals(ttl, options.getTtl());
    Assert.assertTrue(options.hasOwner());
    Assert.assertEquals(owner, options.getOwner());
    Assert.assertTrue(options.hasGroup());
    Assert.assertEquals(group, options.getGroup());
    Assert.assertTrue(options.hasPermission());
    Assert.assertEquals(permission, options.getPermission());
    Assert.assertTrue(options.hasRecursive());
    Assert.assertEquals(recursive, options.getRecursive());
  }
}
