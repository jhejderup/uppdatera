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
import gumtree.spoon.builder.SpoonGumTreeBuilder;
import gumtree.spoon.diff.operations.MoveOperation;
import gumtree.spoon.diff.operations.Operation;
import gumtree.spoon.diff.operations.UpdateOperation;
import net.steppschuh.markdowngenerator.text.emphasis.BoldText;
import spoon.reflect.code.CtStatement;
import spoon.reflect.cu.position.NoSourcePosition;
import spoon.reflect.declaration.CtElement;

import java.util.List;
import java.util.Map;

public final class ResultData {

  public final JVMIdentifier                                 methodID;
  public final List<JVMIdentifier>                           path;
  public final List<Map.Entry<MethodStats, List<Operation>>> changeSet;

  public ResultData(JVMIdentifier methodID, List<JVMIdentifier> path,
      List<Map.Entry<MethodStats, List<Operation>>> changeSet) {
    this.methodID = methodID;
    this.path = path;
    this.changeSet = changeSet;
  }

  public String generateCallTrace() {
    if (path.size() > 0) {

      var calltrace = new StringBuilder(
          this.path.get(0).clazzName.substring(1).replace("/", ".") + "."
              + this.path.get(0).methodName + "\n");

      for (int i = 1; i < this.path.size() - 1; i++) {

        calltrace.append(
            "\tat: " + this.path.get(i).clazzName.substring(1).replace("/", ".")
                + "." + this.path.get(i).methodName + "\n");
      }
      return calltrace.toString();

    } else {
      return "";
    }
  }

  public String generateChangeLogMarkdown() {
    var report = new StringBuilder();
    if (this.changeSet.size() > 0) {

      changeSet.stream().forEach(entry -> {
        var method = entry.getKey();
        var changes = entry.getValue();

        ///
        // General info about the method as such
        ///

        if (method.srcMethod.isPresent() && method.dstMethod.isPresent()) {
          var src = method.srcMethod.get();
          var dst = method.dstMethod.get();

          if (src.equals(dst)) {
            report.append(
                "[![f!](https://img.shields.io/badge/modified-green?style=flat-square)]()");
            int dNum = method.srcNumOfStmts - method.dstNumOfStmts;
            if (dNum > 0) {
              report.append(String.format(
                  "[![f!](https://img.shields.io/badge/+%d-statements-blue?style=flat-square)]()",
                  dNum));
            } else if (dNum < 0) {
              report.append(String.format(
                  "[![f!](https://img.shields.io/badge/-%d-statements-blue?style=flat-square)]()",
                  dNum));
            } else {
              report.append(
                  "[![f!](https://img.shields.io/badge/&#177;0-statements-blue?style=flat-square)]()");
            }
          } else {
            report.append(
                "[![f!](https://img.shields.io/badge/moved-blue?style=flat-square)]()");
            report.append(String.format(
                "[![f!](https://img.shields.io/badge/&#187;-%s-blue?style=flat-square)]()",
                method.dstMethod.get().getSignature()));
          }
        } else {
          report.append("[![f!](https://img.shields.io/badge/deleted-red)]()");
        }

        ///
        // Generate info about changes
        ///

        report.append("<ul>");

        changes.stream().forEach(op -> {

          var nodeType = op.getNode().getClass().getSimpleName();
          var type = nodeType.substring(2, nodeType.length() - 4);

          var el = new StringBuilder(
              op.getAction().getClass().getSimpleName() + " " + type);

          var parent = op.getNode().getParent(e -> e instanceof CtStatement)
              .getClass().getSimpleName();
          var parType = parent.substring(2, parent.length() - 4);
          el.append(" in " + parType);
          if (op.getNode().getPosition() != null && !(op.getNode()
              .getPosition() instanceof NoSourcePosition)) {
            el.append(" (L" + op.getNode().getPosition().getLine() + ")");
          }
          if (op instanceof UpdateOperation || op instanceof MoveOperation) {
            var elementDest = (CtElement) op.getAction().getNode()
                .getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT_DEST);

            var parDst = elementDest.getParent(e -> e instanceof CtStatement)
                .getClass().getSimpleName();
            var parDstType = parDst.substring(2, parDst.length() - 4);
            el.append(" to " + parDstType);

            if (elementDest.getPosition() != null && !(elementDest
                .getPosition() instanceof NoSourcePosition)) {
              el.append(" (L" + elementDest.getPosition().getLine() + ")");
            }

          }

          report.append(String.format("<li>%s</li>", el.toString()));
        }); report.append("</ul>");

      });

    } return report.toString();

  }

  public String generateCallTraceMarkdown() {
    if (path.size() > 0) {

      var calltrace = new StringBuilder("<pre><code>");

      int last = this.path.size() - 1;

      calltrace.append(
          "**" + this.path.get(last).clazzName.substring(1).replace("/", ".")
              + "." + this.path.get(last).methodName + "**\n");

      for (int i = last - 1; i >= 0; i--) {

        calltrace.append("&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;at: " + this.path
            .get(i).clazzName.substring(1).replace("/", ".") + "." + this.path
            .get(i).methodName + "\n");
      }

      calltrace.append("</code></pre>");

      return calltrace.toString();

    } else {
      return "";
    }
  }
}
