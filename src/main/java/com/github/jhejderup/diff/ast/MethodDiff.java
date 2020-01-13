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
import com.github.jhejderup.MethodChange;
import com.github.jhejderup.MethodStats;
import com.github.jhejderup.artifact.JVMIdentifier;
import com.github.jhejderup.diff.file.FileDiff;
import gumtree.spoon.builder.SpoonGumTreeBuilder;
import gumtree.spoon.diff.Diff;
import gumtree.spoon.diff.operations.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spoon.reflect.code.CtStatement;
import spoon.reflect.declaration.*;

import java.util.*;
import java.util.stream.Collectors;

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
        || op instanceof DeleteOperation || op instanceof MoveOperation;
  }

  private MethodChange processInserts(InsertOperation op) {

    var mapping = editScript.getMappingsComp();
    // op node is dst side
    var dstNode = op.getSrcNode();

    // get top level method
    var dstMethodOpt = getTopLevelMethod(dstNode);

    if (!dstMethodOpt.isPresent()) {
      return null;
    } else {

      // get gum tree node
      var dstMethod = dstMethodOpt.get();
      var dstMethodTree = (ITree) dstMethod
          .getMetadata(SpoonGumTreeBuilder.GUMTREE_NODE);

      // map dst -> src (if exists)
      if (!mapping.hasDst(dstMethodTree)) {
        // this inserted element is a part of another inserted element
        // if there is no mapping, this is a new method
        // we only track CHANGED methods (not new) hence there is no mapping
        return null;
      }
      var srcMethodTree = mapping.getSrc(dstMethodTree);
      var srcMethod = (CtElement) srcMethodTree
          .getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);

      var srcStmts = 0;
      try {
        srcStmts = ((CtExecutable) srcMethod).getBody()
            .getElements(el -> el instanceof CtStatement).size();
      } catch (Exception e) {
        logger.error(
            "No Statements in " + ((CtExecutable) srcMethod).getSimpleName());
      }

      var dstStmts = 0;
      try {
        dstStmts = ((CtExecutable) srcMethod).getBody()
            .getElements(el -> el instanceof CtStatement).size();
      } catch (Exception e) {
        logger.error(
            "No Statements in " + ((CtExecutable) srcMethod).getSimpleName());
      }

      var ms = new MethodStats(srcStmts, dstStmts,
          Optional.of(JVMIdentifier.SpoonToJVMString((CtExecutable) srcMethod)),
          Optional
              .of(JVMIdentifier.SpoonToJVMString((CtExecutable) dstMethod)));
      return new MethodChange(op, ms);
    }

  }

  private MethodChange processDeletions(DeleteOperation op) {
    var mapping = editScript.getMappingsComp();
    // op node is src side
    var srcNode = op.getSrcNode();

    // get top level method
    var srcMethodOpt = getTopLevelMethod(srcNode);

    if (!srcMethodOpt.isPresent()) {
      return null;
    } else {
      var srcMethod = srcMethodOpt.get();
      var srcMethodTree = (ITree) srcMethod
          .getMetadata(SpoonGumTreeBuilder.GUMTREE_NODE);

      // src -> dst mapping
      if (!mapping.hasSrc(srcMethodTree)) {
        // this implies that the function is also deleted and hence not
        // mapped in the new version!
        var srcStmts = 0;
        try {
          srcStmts = ((CtExecutable) srcMethod).getBody()
              .getElements(el -> el instanceof CtStatement).size();
        } catch (Exception e) {
          logger.error(
              "No Statements in " + ((CtExecutable) srcMethod).getSimpleName());
        }

        var ms = new MethodStats(srcStmts, 0, Optional
            .of(JVMIdentifier.SpoonToJVMString((CtExecutable) srcMethod)),
            Optional.empty());
        return new MethodChange(op, ms);
      }

      var dstMethodTree = mapping.getDst(srcMethodTree);
      var dstMethod = (CtElement) dstMethodTree
          .getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);

      // stats
      var srcStmts = 0;
      try {
        srcStmts = ((CtExecutable) srcMethod).getBody()
            .getElements(el -> el instanceof CtStatement).size();
      } catch (Exception e) {
        logger.error(
            "No Statements in " + ((CtExecutable) srcMethod).getSimpleName());
      }

      var dstStmts = 0;
      try {
        dstStmts = ((CtExecutable) srcMethod).getBody()
            .getElements(el -> el instanceof CtStatement).size();
      } catch (Exception e) {
        logger.error(
            "No Statements in " + ((CtExecutable) srcMethod).getSimpleName());
      }

      var ms = new MethodStats(srcStmts, dstStmts,
          Optional.of(JVMIdentifier.SpoonToJVMString((CtExecutable) srcMethod)),
          Optional
              .of(JVMIdentifier.SpoonToJVMString((CtExecutable) dstMethod)));
      return new MethodChange(op, ms);

    }

  }

  private MethodChange processUpdatesAndMoves(Operation op) {
    assert op instanceof UpdateOperation || op instanceof MoveOperation;

    var mapping = editScript.getMappingsComp();
    // op node is dst side
    var dstNode = op.getDstNode();

    // get top level method
    var dstMethodOpt = getTopLevelMethod(dstNode);

    if (!dstMethodOpt.isPresent()) {
      return null;
    } else {

      // get gum tree node
      var dstMethod = dstMethodOpt.get();
      var dstMethodTree = (ITree) dstMethod
          .getMetadata(SpoonGumTreeBuilder.GUMTREE_NODE);

      // map dst -> src (if exists)
      if (!mapping.hasDst(dstMethodTree)) {
        // An updated node belonging to a new method!
        return null;
      }
      var srcMethodTree = mapping.getSrc(dstMethodTree);
      var srcMethod = (CtElement) srcMethodTree
          .getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);

      // stats
      var srcStmts = 0;
      try {
        srcStmts = ((CtExecutable) srcMethod).getBody()
            .getElements(el -> el instanceof CtStatement).size();
      } catch (Exception e) {
        logger.info(
            "No Statements in " + ((CtExecutable) srcMethod).getSimpleName());
      }
      var dstStmts = 0;
      try {
        dstStmts = ((CtExecutable) srcMethod).getBody()
            .getElements(el -> el instanceof CtStatement).size();
      } catch (Exception e) {
        logger.info(
            "No Statements in " + ((CtExecutable) srcMethod).getSimpleName());
      }

      var ms = new MethodStats(srcStmts, dstStmts,
          Optional.of(JVMIdentifier.SpoonToJVMString((CtExecutable) srcMethod)),
          Optional
              .of(JVMIdentifier.SpoonToJVMString((CtExecutable) dstMethod)));
      return new MethodChange(op, ms);
    }

  }

  public Map<JVMIdentifier, List<Map.Entry<MethodStats, List<Operation>>>> getChangedMethods() {

    var operations = fileDiff.isFileRemoval() ?
        editScript.getAllOperations() :
        editScript.getRootOperations();

    var changedMethods = operations.stream()
        .filter(MethodDiff::isSupportedOperation)
        .filter(MethodDiff::isChangeInMethod).map(op -> {
          if (op instanceof InsertOperation) {
            return processInserts((InsertOperation) op);
          } else if (op instanceof DeleteOperation) {
            return processDeletions((DeleteOperation) op);
          } else if (op instanceof UpdateOperation
              || op instanceof MoveOperation) {
            return processUpdatesAndMoves(op);
          }
          return null;

        }).filter(Objects::nonNull)
        .collect(Collectors.groupingBy(MethodChange::getMethod));

    var groupedMethods = changedMethods.entrySet().stream().collect(Collectors
        .toMap(Map.Entry::getKey,
            m -> m.getValue().stream().map(mc -> mc.change)
                .collect(Collectors.toList())));

    return groupedMethods.entrySet().stream()
        .collect(Collectors.groupingBy(m -> m.getKey().getSrcMethod()));

  }

}
