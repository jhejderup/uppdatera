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
import com.github.jhejderup.artifact.maven.Artifact;
import com.github.jhejderup.artifact.maven.Coordinate;
import com.github.jhejderup.callgraph.WalaCallgraphConstructor;
import com.github.jhejderup.diff.ast.AstComperator;
import com.github.jhejderup.diff.ast.MethodDiff;
import com.github.jhejderup.diff.file.GitDiffer;
import net.steppschuh.markdowngenerator.text.Text;
import net.steppschuh.markdowngenerator.text.emphasis.BoldText;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

public class UppdateraMaven {

  private static Logger logger = LoggerFactory.getLogger(UppdateraMaven.class);

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
      System.exit(50);
    }

    var filenameOldJar = oldJar.get().getFileName().toString();
    if (!clpathDepz.contains(filenameOldJar)) {
      logger.error("[Uppdatera] `" + filenameOldJar
          + "` is not in the dep classpath of `" + clpathDepz + "`");
      System.exit(51);
    }

    ///
    /// 2. Call Graph Generation
    ///
    var cg = WalaCallgraphConstructor.buildCHA(clpathProject, clpathDepz);
    var graph = new Reachability(cg);

    ///
    /// 3. EditScript Generation
    ///
    var methodDiff = GitDiffer.diff(oldSrc.get(), newSrc.get())
        .filter(fd -> fd.isJavaFile()).filter(fd -> fd.isNotTestFile())
        .filter(fd -> fd.isImpactKind()).map(fd -> {

          AstComperator diff = new AstComperator(oldJar.get().toString());
          Optional<Path> srcFile = fd.srcFile;
          Optional<Path> dstFile = fd.dstFile;

          logger.info("Compare File: {} -> {}", srcFile, dstFile);

          try {
            var editScript = fd.isFileRemoval() ?
                diff.compare(diff.getCtType(srcFile.get().toFile()), null) :
                diff.compare(srcFile.get().toFile(), dstFile.get().toFile());

            return new MethodDiff(editScript, fd);

          } catch (Exception e) {
            e.printStackTrace();
            return null;
          }
        }).filter(Objects::nonNull).map(md -> md.getChangedMethods())
        .filter(md -> md.size() > 0) //remove files w/o relevant changes
        .collect(Collectors.toList());

    ///
    /// 4. Reachability Analysis
    ///
    var result = methodDiff.stream().map(md -> md.entrySet().stream()
        .collect(Collectors.toMap(Map.Entry::getKey, e -> {
          var path = graph.search(e.getKey());
          return new ResultData(e.getKey(), path, e.getValue());
        }))).collect(Collectors.toList());

    ///
    /// 5. Gather Stats & Print report
    ///

    var totalChangedFunctions = result.stream()
        .flatMap(md -> md.entrySet().stream()).count();

    var numAffectedFunctions = result.stream()
        .flatMap(md -> md.entrySet().stream())
        .filter(entry -> entry.getValue().path.size() > 0).count();

    //
    // If no affected functions, we exit!
    //
    if (numAffectedFunctions < 1) {
      System.exit(52);
    }

    var affectedStats = result.stream().flatMap(md -> md.entrySet().stream())
        .filter(entry -> entry.getValue().path.size() > 0).map(entry -> {
          var callstack = entry.getValue().path;
          return callstack.get(callstack.size() - 1);
        }).collect(Collectors.groupingBy(Function.identity()));

    /// Starting paragraph
    var report = new StringBuilder().append(new Text(String
        .format("Bumps %s from %s to %s. ",
            oldCoord.groupId + ":" + oldCoord.artifactId, oldCoord.version,
            newCoord.version))).append(new BoldText(String.format(
        "This update introduces changes in %d existing functions: %d of those functions are called by " + affectedStats.keySet().size()+ " function(s) in this project and has the risk of creating potential regression errors.",
        totalChangedFunctions, numAffectedFunctions))).append(new Text(
        " We advice you to review these changes before merging the pull request"))
        .append("\n\n").append(new Text(String.format(
            "Below includes a changelog for the %d affected functions along with a callstack:",
            numAffectedFunctions))).append("\n").append(new Text("<details>"))
        .append("\n").append(new Text("<summary>Affected Project Functions</summary>"))
        .append("\n").append(new Text("<p>")).append("\n\n");

    /// Affected functions

    affectedStats.entrySet().stream().forEach(e -> {

      var percentage = (float)(e.getValue().size() / numAffectedFunctions) * 100;
      report.append(String.format("- %s", String.format(
          "[![f!](https://img.shields.io/static/v1?label=%s&message=%s()&color=informational&style=flat-square)]()[![f!](https://img.shields.io/static/v1?label=changes&message=%s&#37;&color=critical&style=flat-square)]()",
          e.getKey().clazzName.substring(1).replace("/", "."),
          e.getKey().methodName,percentage, e.getValue().size(), numAffectedFunctions)));

    });

    /// Ending paragraph
    report.append(new Text("</p>")).append("\n\n")
        .append(new Text("</details>")).append("\n\n");

    //Start new section
    report.append(new Text("<details>")).append("\n")
        .append(new Text("<summary>Dependency Changelog</summary>")).append("\n")
        .append(new Text("<p>")).append("\n\n");

    /// Each method
    result.stream().flatMap(md -> md.entrySet().stream())
        .filter(entry -> entry.getValue().path.size() > 0)
        .map(entry -> entry.getValue()).forEach(v -> {
      var mid = v.methodID;

      report.append(new Text(String.format(
          "- [![f!](https://img.shields.io/static/v1?label=%s&message=%s()&color=informational&style=flat-square)]()[![f!](https://img.shields.io/badge/-%s:L%s-inactive?style=flat-square)]()",
          mid.clazzName.substring(1).replace("/", "."), mid.methodName,
          mid.method.get().getPosition().getFile().getName(),
          mid.method.get().getPosition().getLine())))
          .append(new Text("<details><summary>Call Stack</summary>"))
          .append(new Text(v.generateCallTraceMarkdown()))
          .append(new Text("</details>"))
          .append(new Text("<details><summary>Diff</summary>"))
          .append(new Text(v.generateChangeLogMarkdown()))
          .append(new Text("</details>")).append("\n");
    });

    /// Ending paragraph
    report.append(new Text("</p>")).append("\n\n")
        .append(new Text("</details>")).append("\n").append("<hr>")
        .append("\n\n");

    /// survey

    report.append(new Text(
        "Did Uppdatera do a good job?  Give this issue a :+1: if it is **useful**, :-1: if it is **not**, and :hand: if **neutral**. "))
        .append("\n\n");

    report.append(new Text("<details>"));
    report.append(
        new Text("<summary>Want to help us or have suggestions?</summary>"))
        .append("\n\n");
    report.append(new Text(
        "We are a group of researchers trying to make automated dependency services more useful and user-friendly for developers. If you have feedback and questions about this, feel free to submit it [here](https://docs.google.com/forms/d/e/1FAIpQLScgYhqcCGeRjRMqErM3d8BDkDq2ASjAP5pP6EfYamQWYbSTiA/viewform?entry.1269199518=)."))
        .append("\n\n");
    report.append(new Text("</details>"));

    try (var out = new PrintWriter("report.md")) {
      out.println(report);
    }

  }

}
