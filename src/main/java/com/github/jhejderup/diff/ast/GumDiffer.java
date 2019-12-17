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

import com.github.gumtreediff.matchers.MappingStore;
import com.github.gumtreediff.tree.ITree;
import com.github.jhejderup.diff.file.FileDiff;
import gumtree.spoon.builder.SpoonGumTreeBuilder;
import gumtree.spoon.diff.Diff;
import gumtree.spoon.diff.operations.InsertOperation;
import gumtree.spoon.diff.operations.Operation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spoon.reflect.declaration.CtConstructor;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtExecutable;
import spoon.reflect.declaration.CtMethod;

import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class GumDiffer implements Differ {

  private static Logger   logger = LoggerFactory.getLogger(GumDiffer.class);
  private final  String[] classpath;

  public GumDiffer(String[] classpath) {
    this.classpath = classpath;
  }

  private boolean isMethod(CtElement element) {
    return element instanceof CtMethod;
  }

  private boolean isConstructor(CtElement element) {
    return element instanceof CtConstructor;
  }

  private boolean isMethodChange(Operation op, MappingStore store) {
    return getTopLevelMethod(op, store).isPresent();
  }

  private Optional<CtElement> getTopLevelMethod(Operation op,
      MappingStore store) {

    CtElement parent = op.getSrcNode();
    Stack<CtElement> stack = new Stack<>();
    try {
      if (op instanceof InsertOperation) {
        ITree dst = (ITree) op.getSrcNode().getMetadata("gtnode");
        ITree dstParent = store.firstMappedDstParent(dst);
        ITree src = store.getSrc(dstParent);
        parent = (CtElement) src.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);

      }
      //1. keep traversing parents in the tree
      //   until we see the sky
      //   and push elements to the stack
      while (parent.getParent() != null) {
        stack.push(parent);
        parent = parent.getParent();
      }
      //2. pop until we find our top-level method
      if (stack.size() > 0) {
        CtElement method = stack.pop();
        while (stack.size() > 0) {
          if (isMethod(method) || isConstructor(method)) {
            return Optional.of(method);
          }
          method = stack.pop();
        }
        return Optional.empty();
      } else {
        return Optional.empty();
      }
    } catch (Exception e) {
      return Optional.empty();
    }
  }

  private AbstractMap.SimpleEntry<MappingStore, Stream<Operation>> ASTDiff(
      FileDiff fd) throws Exception {

    AstComperator diff = new AstComperator(this.classpath);
    Optional<Path> srcFile = fd.srcFile;
    Optional<Path> dstFile = fd.dstFile;

    logger.info("Compare File: {} -> {}", srcFile, dstFile);

    Diff edit = fd.isFileRemoval() ?
        diff.compare(diff.getCtType(srcFile.get().toFile()), null) :
        diff.compare(srcFile.get().toFile(), dstFile.get().toFile());

    //file removal -> allOperations due to comp. with null
    List<Operation> editScript = fd.isFileRemoval() ?
        edit.getAllOperations() :
        edit.getRootOperations();

    return new AbstractMap.SimpleEntry<>(edit.getMappingsComp(),
        editScript.stream());
  }

  private Map<CtExecutable, List<Operation>> methodDiff(FileDiff fd)
      throws Exception {
    var diff = ASTDiff(fd);
    var mappings = diff.getKey();

    //only changes in a method/constructor
    return diff.getValue().filter(op -> isMethodChange(op, mappings)).collect(
        Collectors.groupingBy(
            op -> ((CtExecutable) getTopLevelMethod(op, mappings).get())));
  }

  @Override
  public GumMethodDiff diff(FileDiff fd) {
    try {
      var md = methodDiff(fd);
      return new GumMethodDiff(fd, Optional.of(md));
    } catch (Exception e) {
      e.printStackTrace();
      return new GumMethodDiff(fd, Optional.empty());
    }
  }
}
