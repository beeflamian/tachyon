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

package tachyon.shell.command;

import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.annotation.concurrent.ThreadSafe;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;

import tachyon.Constants;
import tachyon.TachyonURI;
import tachyon.client.file.FileSystem;
import tachyon.client.file.URIStatus;
import tachyon.conf.TachyonConf;
import tachyon.exception.TachyonException;
import tachyon.util.FormatUtils;

/**
 * Displays information for all directories and files directly under the path specified in args.
 */
@ThreadSafe
public final class LsCommand extends WithWildCardPathCommand {

  /**
   * Constructs a new instance to display information for all directories and files directly under
   * the path specified in args.
   *
   * @param conf the configuration for Tachyon
   * @param fs the filesystem of Tachyon
   */
  public LsCommand(TachyonConf conf, FileSystem fs) {
    super(conf, fs);
  }

  @Override
  public String getCommandName() {
    return "ls";
  }

  @Override
  protected int getNumOfArgs() {
    return 1;
  }

  @Override
  protected Options getOptions() {
    return new Options().addOption(RECURSIVE_OPTION);
  }

  /**
   * Displays information for all directories and files directly under the path specified in args.
   *
   * @param path The {@link TachyonURI} path as the input of the command
   * @param recursive Whether list the path recursively
   * @throws IOException if a non-Tachyon related exception occurs
   */
  private void ls(TachyonURI path, boolean recursive) throws IOException {
    List<URIStatus> statuses = listStatusSortedByIncreasingCreationTime(path);
    for (URIStatus status : statuses) {
      String inMemory = "";
      if (!status.isFolder()) {
        if (100 == status.getInMemoryPercentage()) {
          inMemory = "In Memory";
        } else {
          inMemory = "Not In Memory";
        }
      }
      System.out.format(Constants.COMMAND_FORMAT_LS,
          FormatUtils.formatPermission((short) status.getPermission(), status.isFolder()),
          status.getUserName(), status.getGroupName(),
          FormatUtils.getSizeFromBytes(status.getLength()),
          CommandUtils.convertMsToDate(status.getCreationTimeMs()), inMemory, status.getPath());
      if (recursive && status.isFolder()) {
        ls(new TachyonURI(path.getScheme(), path.getAuthority(), status.getPath()), true);
      }
    }
  }

  private List<URIStatus> listStatusSortedByIncreasingCreationTime(TachyonURI path)
      throws IOException {
    List<URIStatus> statuses;
    try {
      statuses = mFileSystem.listStatus(path);
    } catch (TachyonException e) {
      throw new IOException(e.getMessage());
    }
    Collections.sort(statuses, new Comparator<URIStatus>() {
      @Override
      public int compare(URIStatus status1, URIStatus status2) {
        long t1 = status1.getCreationTimeMs();
        long t2 = status2.getCreationTimeMs();
        if (t1 < t2) {
          return -1;
        }
        if (t1 == t2) {
          return 0;
        }
        return 1;
      }
    });
    return statuses;
  }

  @Override
  public void runCommand(TachyonURI path, CommandLine cl) throws IOException {
    ls(path, cl.hasOption("R"));
  }

  @Override
  public String getUsage() {
    return "ls [-R] <path>";
  }

  @Override
  public String getDescription() {
    return "Displays information for all files and directories directly under the specified path."
        + " Specify -R to display files and directories recursively.";
  }
}
