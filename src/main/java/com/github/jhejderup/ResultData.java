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
import spoon.reflect.code.CtBlock;
import spoon.reflect.code.CtStatement;
import spoon.reflect.cu.position.NoSourcePosition;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtExecutable;

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

  private CtElement getExecutableParentNode(CtElement child) {
    var parent = child;
    if (child instanceof CtExecutable) { //is it a method kind?
      return parent;
    } else {
      parent = child.getParent(
          e -> (e instanceof CtStatement || e instanceof CtExecutable));
    }

    // is it a Block? get the parent of that (e.g., IF/SWITCH/FOR ETC)
    if (parent instanceof CtBlock) {
      parent = parent.getParent(
          e -> (e instanceof CtStatement || e instanceof CtExecutable));
    }
    return parent;

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
                "[![f!](https://img.shields.io/badge/modified-orange?style=flat-square)]()");
          } else {
            report.append(
                "[![f!](https://img.shields.io/badge/moved-blue?style=flat-square)]()");
          }
        } else {
          report.append(
              "[![f!](https://img.shields.io/badge/deleted-red?style=flat-square)]()");
        }

        int dNum = changes.size();
        report.append(String.format(
            "[![f!](https://img.shields.io/badge/%d-changes-informational?style=flat-square)]()",
            dNum));

        ///
        // Generate info about changes
        ///

        report.append("<ul>");

        changes.stream().forEach(op -> {

          var nodeType = op.getNode().getClass().getSimpleName();
          var type = nodeType.substring(2, nodeType.length() - 4);

          var el = new StringBuilder(
              op.getAction().getClass().getSimpleName() + " " + type);

          var parent = getExecutableParentNode(op.getNode());
          var parType = parent.getClass().getSimpleName();

          var parentName = parType.substring(2, parType.length() - 4);

          el.append(" in " + parentName);

          //get line number
          if (op.getNode().getPosition() != null && !(op.getNode()
              .getPosition() instanceof NoSourcePosition)) {
            el.append(" (L" + op.getNode().getPosition().getLine() + ")");
          }

          if (op instanceof UpdateOperation || op instanceof MoveOperation) {
            var elementDest = (CtElement) op.getAction().getNode()
                .getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT_DEST);

            var parDst = getExecutableParentNode(elementDest);

            var parDstType = parDst.getClass().getSimpleName();

            var parDstName = parDstType.substring(2, parDstType.length() - 4);
            el.append(" to " + parDstName);

            //get line number
            if (elementDest.getPosition() != null && !(elementDest
                .getPosition() instanceof NoSourcePosition)) {
              el.append(" (L" + elementDest.getPosition().getLine() + ")");
            }
          }

          //          if(op.getNode().toString().split("\n").length < 2){
          //            el.append("<code><pre>" + op.getNode().toString(). + "</pre></code>");
          //          }

          report.append(String.format("<li>%s</li>", el.toString()));
        });
        report.append("</ul>");

      });

    }
    return report.toString();

  }

  public String generateCallTraceMarkdown() {
    if (path.size() > 0) {

      var calltrace = new StringBuilder("<pre><code>");

      int last = this.path.size() - 1;

      calltrace.append(
          "**" + this.path.get(last).clazzName.substring(1).replace("/", ".")
              + "." + this.path.get(last).methodName + "**\n");

      for (int i = last - 2; i >= 0; i--) {

        calltrace.append("&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;at: " + this.path
            .get(i).clazzName.substring(1).replace("/", ".").replace("_","\\_") + "." + this.path
            .get(i).methodName.replace("_","\\_") + "\n");
      }

      calltrace.append("</code></pre>");

      return calltrace.toString();

    } else {
      return "";
    }
  }
}
