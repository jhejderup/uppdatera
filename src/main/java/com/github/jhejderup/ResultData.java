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
package com.github.jhejderup;

import com.github.jhejderup.artifact.JVMIdentifier;
import gumtree.spoon.diff.operations.Operation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ResultData {

  public final JVMIdentifier       methodID;
  public final List<JVMIdentifier> path;
  public final List<Operation>     changes;

  public ResultData(JVMIdentifier methodID, List<JVMIdentifier> path,
      List<Operation> changes) {
    this.methodID = methodID;
    this.path = path;
    this.changes = changes;
  }

  public String generateCallTrace() {
    if (path.size() > 0) {

      var calltrace = new StringBuffer("affected methods: call dump\n");

      for (int i = 0; i < this.path.size() - 1; i++) {

        calltrace.append(
            "\tat: " + this.path.get(i).clazzName.substring(1).replace("/", ".")
                + "." + this.path.get(i).methodName + "\n");
      }
      return calltrace.toString();

    } else {
      return "";
    }
  }

}
