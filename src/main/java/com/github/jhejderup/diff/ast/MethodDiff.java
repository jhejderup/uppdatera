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
package com.github.jhejderup.diff.ast;

import com.github.gumtreediff.tree.ITree;
import com.github.jhejderup.MethodStats;
import com.github.jhejderup.diff.file.FileDiff;
import gumtree.spoon.diff.Diff;
import gumtree.spoon.diff.operations.DeleteOperation;
import gumtree.spoon.diff.operations.InsertOperation;
import gumtree.spoon.diff.operations.Operation;
import gumtree.spoon.diff.operations.UpdateOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spoon.reflect.code.CtStatement;
import spoon.reflect.declaration.*;

import java.util.Objects;
import java.util.Optional;
import java.util.Stack;

public final class MethodDiff {

  private static Logger logger = LoggerFactory.getLogger(MethodDiff.class);

  public final Diff     editScript;
  public final FileDiff fileDiff;

  public MethodDiff(Diff editScript, FileDiff fileDiff) {
    this.editScript = editScript;
    this.fileDiff = fileDiff;
  }

  public static boolean isMethodKind(CtElement el) {
    return el instanceof CtMethod || el instanceof CtConstructor;
  }

  private static boolean isChangeInMethod(Operation op) {
    try {
      var methodSrc = op.getSrcNode().getParent(MethodDiff::isMethodKind);

      if (op.getDstNode() != null) {
        var methodDst = op.getDstNode().getParent(MethodDiff::isMethodKind);
        return methodSrc != null || methodDst != null;
      } else {
        return methodSrc != null;
      }

    } catch (Exception el) {
      return false;
    }
  }

  private static Optional<CtElement> getTopLevelMethod(CtElement node) {
    var parent = node.getParent();
    var stack = new Stack<CtElement>();

    while (parent != null && !(parent instanceof CtPackage) && node != parent) {
      stack.push(parent);
      parent = parent.getParent();
    }

    while (stack.size() > 0) {
      var el = stack.pop();
      if (isMethodKind(el)) {
        return Optional.of(el);
      }
    }
    return Optional.empty();
  }

  private static boolean isSupportedOperation(Operation op) {
    return op instanceof InsertOperation || op instanceof UpdateOperation
        || op instanceof DeleteOperation;
  }

  public void getChangedMethods() {

    var operations = fileDiff.isFileRemoval() ?
        editScript.getAllOperations() :
        editScript.getRootOperations();
    var mapping = editScript.getMappingsComp();

    operations.stream().filter(MethodDiff::isSupportedOperation)
        .filter(MethodDiff::isChangeInMethod).map(op -> {

      if (op instanceof UpdateOperation || op instanceof InsertOperation) {

        var node = op instanceof InsertOperation ?
            op.getSrcNode() :
            op.getDstNode();

        var parentNodeOpt = getTopLevelMethod(node);

        if (!parentNodeOpt.isPresent()) {
          return null;
        } else {

          var dstParentNode = (CtExecutable) parentNodeOpt.get();
          var dstParentTree = (ITree) dstParentNode.getMetadata("gtnode");

          if (!mapping.hasDst(dstParentTree))
            return null;
          var srcParentTree = mapping.getSrc(dstParentTree);
          var srcParentNode = (CtExecutable) srcParentTree
              .getMetadata("spoon_object");
//-1993687807@@getMD5
          return new MethodStats(srcParentNode.getBody()
              .getElements(el -> el instanceof CtStatement).size(),
              dstParentNode.getBody()
                  .getElements(el -> el instanceof CtStatement).size(),
              srcParentNode, dstParentNode);
        }

      } else if (op instanceof DeleteOperation) {

        var node = op.getSrcNode();
        var parentNodeOpt = getTopLevelMethod(node);

        if (!parentNodeOpt.isPresent()) {
          return null;
        } else {
          var srcParentNode = (CtExecutable) parentNodeOpt.get();
          var srcParentTree = (ITree) srcParentNode.getMetadata("gtnode");
          if (!mapping.hasSrc(srcParentTree))
            return null;
          var dstParentTree = mapping.getDst(srcParentTree);
          var dstParentNode = (CtExecutable) dstParentTree
              .getMetadata("spoon_object");

          return new MethodStats(srcParentNode.getBody()
              .getElements(el -> el instanceof CtStatement).size(),
              dstParentNode.getBody()
                  .getElements(el -> el instanceof CtStatement).size(),
              srcParentNode, dstParentNode);
        }
      }
      return null;

    }).filter(Objects::nonNull).forEach(System.out::println);

  }

}
