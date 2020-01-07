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

import com.github.gumtreediff.actions.model.Move;
import com.github.gumtreediff.actions.model.Update;
import com.github.jhejderup.artifact.JVMIdentifier;
import gnu.trove.impl.sync.TSynchronizedShortByteMap;
import gumtree.spoon.builder.SpoonGumTreeBuilder;
import gumtree.spoon.diff.operations.Operation;
import spoon.reflect.code.CtStatement;
import spoon.reflect.cu.position.NoSourcePosition;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtPackage;
import spoon.reflect.declaration.CtType;
import spoon.reflect.visitor.DefaultJavaPrettyPrinter;

import java.util.List;
import java.util.stream.Collectors;

public final class ResultData {

  public final JVMIdentifier       methodID;
  public final int   numOfMethodStatements;
  public final List<JVMIdentifier> path;
  public final List<Operation>     changes;

  public ResultData(JVMIdentifier methodID, List<JVMIdentifier> path,
      List<Operation> changes, int  numOfMethodStatements) {
    this.methodID = methodID;
    this.path = path;
    this.changes = changes;
    this.numOfMethodStatements = numOfMethodStatements;
  }

  public String generateCallTrace() {
    if (path.size() > 0) {


      var calltrace = new StringBuffer(generateChangeLog());

      calltrace.append(this.changes.size() + " change(s) [ "
          + (float) ((this.changes.size() * 100) / this.numOfMethodStatements) + "% ("
          + this.changes.size() + "/" + this.numOfMethodStatements + ")] in "
          + this.path.get(0).clazzName.substring(1).replace("/", ".") + "."
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

  // * Delete function call in return-statement at line 189
  //  return ~~function call~~
  // * Add field read in return statement at line 189
  // return ++messageContents++
  // * Delete if-statement body at line 123
  // - X statements deleted: Fun del, read del,
  // * Add field write assignment of if-statement at line 12
  // - ~~resp~~messageContents = tmp
  // *
  public String generateChangeLog() {
    return this.changes.stream().map(change -> toStringAction(change))
        .collect(Collectors.joining(","));
  }

  private String toStringAction(Operation op) {
    var action = op.getAction();
    var node = op.getNode();
    String newline = System.getProperty("line.separator");
    StringBuilder stringBuilder = new StringBuilder();

    // action name
    stringBuilder.append(action.getClass().getSimpleName());

    CtElement element = node;

    if (element == null) {
      // some elements are only in the gumtree for having a clean diff but not in the Spoon metamodel
      return stringBuilder.toString() + " fake_node(" + action.getNode()
          .getMetadata("type") + ")";
    }

    // node type
    String nodeType = element.getClass().getSimpleName();
    nodeType = nodeType.substring(2, nodeType.length() - 4);
    stringBuilder.append(" ").append(nodeType);

    // action position
    CtElement parent = element;
    while (parent.getParent() != null && !(parent
        .getParent() instanceof CtPackage)) {
      parent = parent.getParent();
    }
    String position = " at ";
    if (parent instanceof CtType) {
      position += ((CtType) parent).getQualifiedName();
    }
    if (element.getPosition() != null && !(element
        .getPosition() instanceof NoSourcePosition)) {
      position += ":" + element.getPosition().getLine();
    }
    if (action instanceof Move) {
      CtElement elementDest = (CtElement) action.getNode()
          .getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT_DEST);
      position = " from " + element.getParent(CtClass.class).getQualifiedName();
      if (element.getPosition() != null && !(element
          .getPosition() instanceof NoSourcePosition)) {
        position += ":" + element.getPosition().getLine();
      }
      position +=
          " to " + elementDest.getParent(CtClass.class).getQualifiedName();
      if (elementDest.getPosition() != null && !(elementDest
          .getPosition() instanceof NoSourcePosition)) {
        position += ":" + elementDest.getPosition().getLine();
      }
    }
    stringBuilder.append(position).append(newline);

    // code change
    String label = partialElementPrint(element);
    if (action instanceof Move) {
      label = element.toString();
    }
    if (action instanceof Update) {
      CtElement elementDest = (CtElement) action.getNode()
          .getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT_DEST);
      label += " to " + elementDest.toString();
    }
    String[] split = label.split(newline);
    for (String s : split) {
      stringBuilder.append("\t").append(s).append(newline);
    }
    return stringBuilder.toString();
  }

  private String partialElementPrint(CtElement element) {
    DefaultJavaPrettyPrinter print = new DefaultJavaPrettyPrinter(
        element.getFactory().getEnvironment()) {
      @Override
      public DefaultJavaPrettyPrinter scan(CtElement e) {
        if (e != null && e.getMetadata("isMoved") == null) {
          return super.scan(e);
        }
        return this;
      }
    };

    print.scan(element);
    return print.getResult();
  }

}
