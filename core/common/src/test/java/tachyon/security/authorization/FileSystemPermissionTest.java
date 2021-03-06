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

package tachyon.security.authorization;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import tachyon.Constants;
import tachyon.conf.TachyonConf;
import tachyon.exception.ExceptionMessage;

/**
 * Tests the {@link FileSystemPermission} class.
 */
public final class FileSystemPermissionTest {

  /**
   * The exception expected to be thrown.
   */
  @Rule
  public ExpectedException mThrown = ExpectedException.none();

  private TachyonConf mConf = new TachyonConf();

  /**
   * Tests the {@link FileSystemPermission#toShort()} method.
   */
  @Test
  public void toShortTest() {
    FileSystemPermission permission =
        new FileSystemPermission(FileSystemAction.ALL, FileSystemAction.READ_EXECUTE,
            FileSystemAction.READ_EXECUTE);
    Assert.assertEquals(0755, permission.toShort());

    permission = FileSystemPermission.getDefault();
    Assert.assertEquals(0777, permission.toShort());

    permission = new FileSystemPermission(FileSystemAction.READ_WRITE, FileSystemAction.READ,
        FileSystemAction.READ);
    Assert.assertEquals(0644, permission.toShort());
  }

  /**
   * Tests the {@link FileSystemPermission#fromShort(short)} method.
   */
  @Test
  public void fromShortTest() {
    FileSystemPermission permission = new FileSystemPermission((short)0777);
    Assert.assertEquals(FileSystemAction.ALL, permission.getUserAction());
    Assert.assertEquals(FileSystemAction.ALL, permission.getGroupAction());
    Assert.assertEquals(FileSystemAction.ALL, permission.getOtherAction());

    permission = new FileSystemPermission((short)0644);
    Assert.assertEquals(FileSystemAction.READ_WRITE, permission.getUserAction());
    Assert.assertEquals(FileSystemAction.READ, permission.getGroupAction());
    Assert.assertEquals(FileSystemAction.READ, permission.getOtherAction());

    permission = new FileSystemPermission((short)0755);
    Assert.assertEquals(FileSystemAction.ALL, permission.getUserAction());
    Assert.assertEquals(FileSystemAction.READ_EXECUTE, permission.getGroupAction());
    Assert.assertEquals(FileSystemAction.READ_EXECUTE, permission.getOtherAction());
  }

  /**
   * Tests the {@link FileSystemPermission#FileSystemPermission(FileSystemPermission)} constructor.
   */
  @Test
  public void copyConstructorTest() {
    FileSystemPermission permission = new FileSystemPermission(FileSystemPermission.getDefault());
    Assert.assertEquals(FileSystemAction.ALL, permission.getUserAction());
    Assert.assertEquals(FileSystemAction.ALL, permission.getGroupAction());
    Assert.assertEquals(FileSystemAction.ALL, permission.getOtherAction());
    Assert.assertEquals(0777, permission.toShort());
  }

  /**
   * Tests the {@link FileSystemPermission#getNoneFsPermission()} method.
   */
  @Test
  public void getNoneFsPermissionTest() {
    FileSystemPermission permission = FileSystemPermission.getNoneFsPermission();
    Assert.assertEquals(FileSystemAction.NONE, permission.getUserAction());
    Assert.assertEquals(FileSystemAction.NONE, permission.getGroupAction());
    Assert.assertEquals(FileSystemAction.NONE, permission.getOtherAction());
    Assert.assertEquals(0000, permission.toShort());
  }

  /**
   * Tests the {@link FileSystemPermission#equals(Object)} method.
   */
  @Test
  public void equalsTest() {
    FileSystemPermission allPermission = new FileSystemPermission((short)0777);
    Assert.assertTrue(allPermission.equals(FileSystemPermission.getDefault()));
    FileSystemPermission nonePermission = new FileSystemPermission((short)0000);
    Assert.assertTrue(nonePermission.equals(FileSystemPermission.getNoneFsPermission()));
    Assert.assertFalse(allPermission.equals(nonePermission));
  }

  /**
   * Tests the {@link FileSystemPermission#toString()} method.
   */
  @Test
  public void toStringTest() {
    Assert.assertEquals("rwxrwxrwx", new FileSystemPermission((short)0777).toString());
    Assert.assertEquals("rw-r-----", new FileSystemPermission((short)0640).toString());
    Assert.assertEquals("rw-------", new FileSystemPermission((short)0600).toString());
    Assert.assertEquals("---------", new FileSystemPermission((short)0000).toString());
  }

  /**
   * Tests the {@link FileSystemPermission#getUMask(TachyonConf)} and
   * {@link FileSystemPermission#applyUMask(FileSystemPermission)} methods.
   */
  @Test
  public void umaskTest() {
    String umask = "0022";
    mConf.set(Constants.SECURITY_AUTHORIZATION_PERMISSIONS_UMASK, umask);
    FileSystemPermission umaskPermission = FileSystemPermission.getUMask(mConf);
    // after umask 0022, 0777 should change to 0755
    FileSystemPermission permission = FileSystemPermission.getDefault().applyUMask(umaskPermission);
    Assert.assertEquals(FileSystemAction.ALL, permission.getUserAction());
    Assert.assertEquals(FileSystemAction.READ_EXECUTE, permission.getGroupAction());
    Assert.assertEquals(FileSystemAction.READ_EXECUTE, permission.getOtherAction());
    Assert.assertEquals(0755, permission.toShort());
  }

  /**
   * Tests the {@link FileSystemPermission#getUMask(TachyonConf)} method to thrown an exception when
   * it exceeds the length.
   */
  @Test
  public void umaskExceedLengthTest() {
    String umask = "00022";
    mConf.set(Constants.SECURITY_AUTHORIZATION_PERMISSIONS_UMASK, umask);
    mThrown.expect(IllegalArgumentException.class);
    mThrown.expectMessage(ExceptionMessage.INVALID_CONFIGURATION_VALUE.getMessage(umask,
        Constants.SECURITY_AUTHORIZATION_PERMISSIONS_UMASK));
    FileSystemPermission.getUMask(mConf);
  }

  /**
   * Tests the {@link FileSystemPermission#getUMask(TachyonConf)} method to thrown an exception when
   * it is not an integer.
   */
  @Test
  public void umaskNotIntegerTest() {
    String umask = "NotInteger";
    mConf.set(Constants.SECURITY_AUTHORIZATION_PERMISSIONS_UMASK, umask);
    mThrown.expect(IllegalArgumentException.class);
    mThrown.expectMessage(ExceptionMessage.INVALID_CONFIGURATION_VALUE.getMessage(umask,
        Constants.SECURITY_AUTHORIZATION_PERMISSIONS_UMASK));
    FileSystemPermission.getUMask(mConf);
  }
}
