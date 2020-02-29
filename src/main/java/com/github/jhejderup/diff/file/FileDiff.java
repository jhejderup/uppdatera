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

import java.io.Serializable;
import java.nio.file.Path;
import java.util.Optional;

public final class FileDiff implements Serializable {
  public final Optional<Path> srcFile;
  public final Optional<Path> dstFile;
  public final Change         type;

  public FileDiff(Optional<Path> srcFile, Optional<Path> dstFile, Change type) {
    this.srcFile = srcFile;
    this.dstFile = dstFile;
    this.type = type;
  }

  public static Change getChangeType(String statusCode) {

    if (statusCode.startsWith("M")) {
      return Change.MODIFICATION;
    } else if (statusCode.startsWith("D")) {
      return Change.DELETION;
    } else if (statusCode.startsWith("A")) {
      return Change.ADDITION;
    } else if (statusCode.startsWith("R")) {
      Change type = FileDiff.Change.RENAME;
      type.setPercentage(Integer.parseInt(statusCode.substring(1)));
      return type;
    } else if (statusCode.startsWith("C")) {
      Change type = FileDiff.Change.COPY;
      type.setPercentage(Integer.parseInt(statusCode.substring(1)));
      return type;
    } else {
      return Change.UNKNOWN;
    }
  }

  public boolean isImpactKind() {
    return this.type == FileDiff.Change.DELETION
        || this.type == FileDiff.Change.RENAME
        || this.type == FileDiff.Change.MODIFICATION;
  }

  public boolean isFileRemoval() {
    return this.type == FileDiff.Change.DELETION
        || this.type == FileDiff.Change.RENAME;
  }

  public boolean isJavaFile() {
    return this.srcFile.map(path -> path.toString().endsWith(".java"))
        .orElse(false);
  }

  public boolean isTestFile() {
    // TODO: Write a test for this
    return this.srcFile.map(path -> path.toString().contains("/src/test/"))
        .orElse(false);
  }

  @Override
  public String toString() {
    return "FileDiff(" + type + "," + srcFile.toString() + "," + dstFile
        .toString() + ")";
  }

  public enum Change {
    MODIFICATION(100), ADDITION(100), DELETION(100), COPY(0), RENAME(
        0), UNKNOWN(0);
    private int percentage;

    Change(int percentage) {
      this.percentage = percentage;
    }

    public void setPercentage(int percentage) {
      this.percentage = percentage;
    }

    @Override
    public String toString() {
      return this.name() + "(" + this.percentage + ")";
    }
  }
}

