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
import gumtree.spoon.builder.SpoonGumTreeBuilder;
import gumtree.spoon.diff.Diff;
import gumtree.spoon.diff.operations.*;
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

    if (isMethodKind(node)) {
      return Optional.of(node);
    }
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

      if (op instanceof InsertOperation) {
        // op node is dst side
        var dstNode = op.getSrcNode();

        var dstNodeTree = (ITree) dstNode
            .getMetadata(SpoonGumTreeBuilder.GUMTREE_NODE);
        var dstNodeParentTree = mapping.firstMappedDstParent(dstNodeTree);
        var srcNodeParentTree = mapping.getSrc(dstNodeParentTree);

        // map dst -> src (if exists)
        var dstNodeParent = (CtElement) dstNodeParentTree
            .getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);
        var srcNodeParent = (CtElement) srcNodeParentTree
            .getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);

        // find top level method
        var dstMethodOpt = getTopLevelMethod(dstNodeParent);
        var srcMethodOpt = getTopLevelMethod(srcNodeParent);

        // gen stats
        if (dstMethodOpt.isPresent()) {
          var dstMethod = dstMethodOpt.get();
          var srcMethod = srcMethodOpt.isPresent() ? srcMethodOpt.get() : null;

          if (srcMethod != null) {
            return new MethodStats(((CtExecutable) srcMethod).getBody()
                .getElements(el -> el instanceof CtStatement).size(),
                ((CtExecutable) dstMethod).getBody()
                    .getElements(el -> el instanceof CtStatement).size(),
                (CtExecutable) srcMethod, (CtExecutable) dstMethod);
          } else {
            return new MethodStats(0, ((CtExecutable) dstMethod).getBody()
                .getElements(el -> el instanceof CtStatement).size(), null,
                (CtExecutable) dstMethod);
          }

        } else {
          return null;
        }

      } else if (op instanceof DeleteOperation) {
        return null;

      } else if (op instanceof UpdateOperation) {
        return null;

      } else if (op instanceof MoveOperation) {
        return null;
      }
      return null;

    }).filter(Objects::nonNull).forEach(System.out::println);

  }

  //  public void getChangedMethods() {
  //
  //    var operations = fileDiff.isFileRemoval() ?
  //        editScript.getAllOperations() :
  //        editScript.getRootOperations();
  //    var mapping = editScript.getMappingsComp();
  //
  //    operations.stream().filter(MethodDiff::isSupportedOperation)
  //        .filter(MethodDiff::isChangeInMethod).map(op -> {
  //
  //      if (op instanceof UpdateOperation || op instanceof InsertOperation) {
  //
  //        var dstNode = op.getSrcNode();
  //        if (op instanceof InsertOperation) {
  //          // we take the corresponding node in the source tree
  //          dstNode = (CtElement) op.getAction().getNode().getParent()
  //              .getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);
  //        }
  //
  //        var dstTree = (ITree) dstNode.getMetadata("gtnode");
  //
  //        if (!mapping.hasDst(dstTree))
  //          return null;
  //
  //        var srcTree = mapping.getSrc(dstTree);
  //        var srcNode = (CtElement) srcTree.getMetadata("spoon_object");
  //
  //        var srcNodeParentOpt = getTopLevelMethod(srcNode);
  //        var dstNodeParentOpt = getTopLevelMethod(dstNode);
  //
  //
  //
  //        if (srcNodeParentOpt.isPresent() && dstNodeParentOpt.isPresent()) {
  //          var srcNodeParent = (CtExecutable) srcNodeParentOpt.get();
  //          var dstNodeParent = (CtExecutable) dstNodeParentOpt.get();
  //          return new MethodStats(srcNodeParent.getBody()
  //              .getElements(el -> el instanceof CtStatement).size(),
  //              dstNodeParent.getBody()
  //                  .getElements(el -> el instanceof CtStatement).size(),
  //              srcNodeParent, dstNodeParent);
  //        } else {
  //          return null;
  //        }
  //
  //      } else if (op instanceof DeleteOperation) {
  //        return null;
  //      }
  //      return null;
  //
  //    }).filter(Objects::nonNull).forEach(System.out::println);
  //
  //  }

}
