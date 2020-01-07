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

import com.github.jhejderup.analysis.Reachability;
import com.github.jhejderup.artifact.JVMIdentifier;
import com.github.jhejderup.artifact.maven.Artifact;
import com.github.jhejderup.artifact.maven.Coordinate;
import com.github.jhejderup.callgraph.WalaCallgraphConstructor;
import com.github.jhejderup.diff.ast.AstComperator;
import com.github.jhejderup.diff.ast.GumDiffer;
import com.github.jhejderup.diff.ast.MethodDiff;
import com.github.jhejderup.diff.file.GitDiffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spoon.reflect.code.CtStatement;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtExecutable;
import spoon.reflect.declaration.CtParameter;
import spoon.reflect.declaration.CtType;
import spoon.reflect.reference.CtArrayTypeReference;
import spoon.reflect.reference.CtTypeReference;
import spoon.reflect.visitor.Filter;

import java.io.IOException;
import java.nio.file.Path;
import java.sql.PreparedStatement;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class UppdateraMaven {

  public static  Map<String, String> spoonToJVM = new HashMap<>();
  private static Logger              logger     = LoggerFactory
      .getLogger(UppdateraMaven.class);

  static {
    spoonToJVM.put("byte", "B");
    spoonToJVM.put("char", "C");
    spoonToJVM.put("double", "D");
    spoonToJVM.put("float", "F");
    spoonToJVM.put("int", "I");
    spoonToJVM.put("long", "J");
    spoonToJVM.put("short", "S");
    spoonToJVM.put("boolean", "Z");
    spoonToJVM.put("void", "V");
  }

  private static JVMIdentifier SpoonToJVMString(CtExecutable item) {
    var clazz = ((CtType) (item.getParent())).getReference();
    var ret = item.getType();
    var args = Arrays.stream(item.getParameters().toArray())
        .map(CtParameter.class::cast).map(arg -> toJVMType(arg.getType(), true))
        .collect(Collectors.joining(""));

    return new JVMIdentifier(toJVMType(clazz, false), item.getSimpleName(),
        "(" + args + ")" + toJVMType(ret, true));

  }

  private static String toJVMType(CtTypeReference type, boolean isMethodDesc) {
    var JVMType = new StringBuilder();

    if (type instanceof CtArrayTypeReference) {
      var brackets = ((CtArrayTypeReference) type).getDimensionCount();
      IntStream.rangeClosed(1, brackets).forEach(i -> JVMType.append("["));
    }

    if (type.isPrimitive()) {
      JVMType.append(spoonToJVM.get(type.getSimpleName()));
    } else {
      JVMType.append("L");
      JVMType.append(type.getQualifiedName().replace(".", "/"));
      if (isMethodDesc)
        JVMType.append(";");
    }
    return JVMType.toString();
  }

  //////////
  /// uppdatera <args>
  /// - [0] classpath_project : path to target/classes
  /// - [1] classpath_deps: path to all dependencies
  /// - [2] groupid
  /// - [3] artifactid
  /// - [4] version old
  /// - [5] version new
  //////////
  public static void main(String[] args) throws IOException {
    assert args.length == 6;

    var clpathProject = args[0];
    var clpathDepz = args[1];

    ///
    /// 1. Validate and download artifacts
    ///
    var oldCoord = new Coordinate(args[2], args[3], args[4]);
    var newCoord = new Coordinate(args[2], args[3], args[5]);

    var oldArtifact = new Artifact(oldCoord);
    var newArtifact = new Artifact(newCoord);

    var oldSrc = oldArtifact.getSource();
    var oldJar = oldArtifact.getBinary();
    var newSrc = newArtifact.getSource();

    if (!oldSrc.isPresent() || !newSrc.isPresent() || !oldJar.isPresent()) {
      if (!oldSrc.isPresent())
        logger.error("[Uppdatera] Unable to download source for " + oldCoord);
      if (!newSrc.isPresent())
        logger.error("[Uppdatera] Unable to  download source for  " + newCoord);
      if (!oldJar.isPresent())
        logger.error("[Uppdatera] Unable to download jar file for " + oldCoord);
      return;
    }

    var filenameOldJar = oldJar.get().getFileName().toString();
    if (!clpathDepz.contains(filenameOldJar)) {
      logger.error("[Uppdatera] `" + filenameOldJar
          + "` is not in the dep classpath of `" + clpathDepz + "`");
      return;
    }

    ///
    /// 2. Call Graph Generation
    ///
    var cg = WalaCallgraphConstructor.buildCHA(clpathProject, clpathDepz);
    var graph = new Reachability(cg);


    GitDiffer.diff(oldSrc.get(), newSrc.get())
        .filter(fd -> fd.isJavaFile()).filter(fd -> fd.isNotTestFile())
        .filter(fd -> fd.isImpactKind())
        .map(fd -> {

          AstComperator diff = new AstComperator(new String[] { oldJar.get().toString() });
          Optional<Path> srcFile = fd.srcFile;
          Optional<Path> dstFile = fd.dstFile;

          logger.info("Compare File: {} -> {}", srcFile, dstFile);

          try {
            var editScript = fd.isFileRemoval() ?
                diff.compare(diff.getCtType(srcFile.get().toFile()), null) :
                diff.compare(srcFile.get().toFile(), dstFile.get().toFile());

            return new MethodDiff(editScript,fd);

          } catch (Exception e) {
            e.printStackTrace();
            return null;
          }
        }).forEach(md -> md.getChangedMethods());



    //    ///
//    /// 3. Diffing & Reachability Analysis
//    ///
//    var gumDiff = new GumDiffer(new String[] { oldJar.get().toString() });
//
//    var result = GitDiffer.diff(oldSrc.get(), newSrc.get())
//        .filter(fd -> fd.isJavaFile()).filter(fd -> fd.isNotTestFile())
//        .filter(fd -> fd.isImpactKind()).map(fd -> gumDiff.diff(fd))
//
//        .filter(gd -> gd.methodDiffs.isPresent())
//        .flatMap(gd -> gd.methodDiffs.get().entrySet().stream().map(md -> {
//          var methodID = SpoonToJVMString(md.getKey());
//          var path = graph.search(methodID);
//          var numOfStmts = md.getKey().getBody().getElements(k -> k instanceof CtStatement).stream().count();
//          return new ResultData(methodID, path, md.getValue(), (int) numOfStmts);
//        })).collect(Collectors.toList());
//
//    ///
//    /// 4. Generate a report
//    ///
//    result.stream().map(k -> k.generateCallTrace()).forEach(System.out::println);
  }


}
