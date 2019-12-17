/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.jhejderup.diff.file;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeroturnaround.exec.ProcessExecutor;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeoutException;
import java.util.stream.Stream;

public final class GitDiffer implements Differ {
  private static Logger logger = LoggerFactory
      .getLogger(GitDiffer.class);
  private static String execute(Path leftFolder, Path rightFolder)
      throws InterruptedException, TimeoutException, IOException {
    return new ProcessExecutor()
        .command("git", "diff", "--no-index", "--name-status",
            leftFolder.toString(), rightFolder.toString()).readOutput(true)
        .execute().outputString();
  }

  public static Stream<FileDiff> diff(Path leftFolder, Path rightFolder) {
    String rawOutput = null;
    try {
      rawOutput = execute(leftFolder, rightFolder);
    } catch (Exception e) {
      logger.error("[GitDiffer] failed to run native git diff");
      e.printStackTrace();
      return Stream.empty();
    }
    return Arrays.stream(rawOutput.split("\n")).map(line -> line.split("\t"))
        .map(arr -> {
          String srcFile = arr[1];
          FileDiff.Change mode = FileDiff.getChangeType(arr[0]);
          if (arr.length == 3) {
            String dstFile = arr[2];
            return new FileDiff(Optional.of(Paths.get(srcFile)),
                Optional.of(Paths.get(dstFile)), mode);
          } else {
            switch (mode) {
            case MODIFICATION:
              String filename = srcFile.replace(leftFolder.toString(), "");
              return new FileDiff(Optional.of(Paths.get(srcFile)),
                  Optional.of(Paths.get(rightFolder + filename)), mode);
            case DELETION:
              return new FileDiff(Optional.of(Paths.get(srcFile)),
                  Optional.empty(), mode);
            case ADDITION:
              return new FileDiff(Optional.empty(),
                  Optional.of(Paths.get(srcFile)), mode);
            default:
              return null;
            }
          }
        }).filter(Objects::nonNull); //we skip unknown (null diffs)
  }
}
