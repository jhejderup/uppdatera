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
import com.github.jhejderup.callgraph.CallgraphConstructor;
import com.github.jhejderup.callgraph.CallgraphException;
import com.github.jhejderup.callgraph.JVMIdentifier;
import com.github.jhejderup.callgraph.wala.WalaCallgraphConstructor;
import com.github.jhejderup.diff.ast.AstComperator;
import com.github.jhejderup.diff.ast.MethodDiff;
import com.github.jhejderup.diff.file.FileDiff;
import com.github.jhejderup.diff.file.GitDiffer;
import gumtree.spoon.builder.SpoonGumTreeBuilder;
import gumtree.spoon.diff.operations.MoveOperation;
import gumtree.spoon.diff.operations.Operation;
import gumtree.spoon.diff.operations.UpdateOperation;
import net.steppschuh.markdowngenerator.text.Text;
import net.steppschuh.markdowngenerator.text.emphasis.BoldText;
import net.steppschuh.markdowngenerator.text.emphasis.ItalicText;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spoon.reflect.code.CtBlock;
import spoon.reflect.code.CtStatement;
import spoon.reflect.cu.position.NoSourcePosition;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtExecutable;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
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
    public static void main(String[] args) throws IOException, CallgraphException {
        assert args.length == 6;

        var projectClasspath = args[0];
        var depzClasspath = args[1];

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

        if (oldSrc.isEmpty() || newSrc.isEmpty() || oldJar.isEmpty()) {
            if (oldSrc.isEmpty())
                logger.error("[Uppdatera] Unable to download source for " + oldCoord);
            if (newSrc.isEmpty())
                logger.error("[Uppdatera] Unable to download source for  " + newCoord);
            if (oldJar.isEmpty())
                logger.error("[Uppdatera] Unable to download jar file for " + oldCoord);
            System.exit(50);
        }

        var filenameOldJar = oldJar.get().getFileName().toString();
        if (!depzClasspath.contains(filenameOldJar)) {
            logger.error("[Uppdatera] `" + filenameOldJar
                    + "` is not in the dep classpath of `" + depzClasspath + "`");
            System.exit(51);
        }

        ///
        /// 2. Call Graph Generation
        ///
        CallgraphConstructor cgConstructor = new WalaCallgraphConstructor(); // Can be switched to Opal callgraph in the future.
        var resolvedCalls = cgConstructor.build(projectClasspath, depzClasspath);

        logger.info("[Uppdatera] Done setting up call graph");

        var graph = new Reachability(resolvedCalls);

        logger.info("[Uppdatera] Done setting up reachability analysis");

        ///
        /// 3. EditScript Generation
        ///
        var methodDiff = GitDiffer.diff(oldSrc.get(), newSrc.get())
                .filter(FileDiff::isJavaFile).filter(fd -> !fd.isTestFile())
                .filter(FileDiff::isImpactKind).map(fd -> {

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
                }).filter(Objects::nonNull).map(MethodDiff::getChangedMethods)
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
                .mapToLong(md -> md.entrySet().size()).sum();

        var numAffectedFunctions = result.stream()
                .flatMap(md -> md.entrySet().stream())
                .filter(entry -> entry.getValue().path.size() > 0).count();

        //
        // If no affected functions, we exit!
        //
        if (numAffectedFunctions < 1) {
            System.exit(52);
        }

        var affectedFunctions = result.stream()
                .flatMap(md -> md.entrySet().stream())
                .filter(entry -> entry.getValue().path.size() > 0)
                .collect(Collectors.groupingBy(e -> {
                    var callstack = e.getValue().path;
                    return callstack.get(callstack.size() - 1);
                }));

        /// Starting paragraph
        var report = new StringBuilder().append(new Text(String
                .format("Bumps %s from %s to %s. ",
                        oldCoord.groupId + ":" + oldCoord.artifactId, oldCoord.version,
                        newCoord.version))).append(new BoldText(String.format(
                "This update introduces changes in %d existing functions: %d of those functions are called by "
                        + affectedFunctions.keySet().size()
                        + " function(s) in this project and has the risk of creating potential regression errors.",
                totalChangedFunctions, numAffectedFunctions))).append(new Text(
                " We advise you to review these changes before merging the pull request."))
                .append("\n\n").append(new Text(
                        "Below are project functions that will be impacted after the update:"))
                .append("\n");

        /// iterate affected functions
        affectedFunctions.entrySet().stream().forEach(entry -> {

            var mid = entry.getKey();
            var traceb = new StringBuilder();

            var changeSize = entry.getValue().size();

            var targetMethods = new HashSet<JVMIdentifier>();

            if (changeSize > 3) {

                var r = new Random().ints(0, changeSize).distinct().limit(3).toArray();
                traceb.append(
                        entry.getValue().get(r[0])
                                .getValue().generateCallTraceMarkdown());
                traceb.append(
                        entry.getValue().get(r[1])
                                .getValue().generateCallTraceMarkdown());
                traceb.append(
                        entry.getValue().get(r[2])
                                .getValue().generateCallTraceMarkdown());

                targetMethods.add(entry.getValue().get(r[0]).getKey());
                targetMethods.add(entry.getValue().get(r[1]).getKey());
                targetMethods.add(entry.getValue().get(r[2]).getKey());
            } else {
                traceb.append(
                        entry.getValue().get(0).getValue().generateCallTraceMarkdown());
                targetMethods.add(entry.getValue().get(0).getKey());
            }

            report.append(new Text(String.format(
                    "- [![f!](https://img.shields.io/static/v1?label=%s&message=%s()&color=informational&style=flat-square)]()[![f!](https://img.shields.io/badge/&#x21A6;-black?style=flat-square)]()[![f!](https://img.shields.io/static/v1?label=%s&message=reachable&nbsp;dep&nbsp;function(s)&color=critical&style=flat-square)]()",
                    mid.clazzName.substring(1).replace("/", ".").replace("_", "\\_"), mid.methodName.replace("_", "\\_"),
                    entry.getValue().size()))).append(
                    new Text("<details><summary>Sample Affected Path(s)</summary>"))
                    .append(traceb.toString()).append(new Text("</details>")).append(
                    new Text("<details><summary>Changed Dependency Function(s)</summary>"))
                    .append(new Text(formatChanges(entry.getValue(), targetMethods)))
                    .append(new Text("</details>")).append("\n");

        });

        report.append("<hr>").append("\n\n");
        /// survey

        report.append(new Text(
                "Did you find this information useful?  Give this issue a :+1: if it is **useful**, :-1: if it is **not**, and :hand: if **neutral**. "))
                .append("\n\n");

        report.append(new Text("<details>"));
        report.append(
                new Text("<summary>Want to help us or have suggestions?</summary>"))
                .append("\n\n");
        report.append(new Text(
                "We are a group of university researchers trying to make automated dependency services more useful and user-friendly for OSS projects. If you have feedback and questions about this, feel free to submit it [here](https://docs.google.com/forms/d/e/1FAIpQLScgYhqcCGeRjRMqErM3d8BDkDq2ASjAP5pP6EfYamQWYbSTiA/viewform?entry.1269199518=)."))
                .append("\n\n");
        report.append(new Text("</details>"));

        try (var out = new PrintWriter("report.md")) {
            out.println(report);
        }

    }

    private static String formatChanges(
            List<Map.Entry<JVMIdentifier, ResultData>> changeSet, HashSet<JVMIdentifier> match) {
        var report = new StringBuilder();

        report.append("<ul>");

        var list = new ArrayList<String>();
        var special = new ArrayList<String>();

        changeSet.forEach(ch -> {

            var change = ch.getValue().changeSet.get(0);
            var method = change.getKey();
            var sb = new StringBuilder();

            if (method.srcMethod.isPresent() && method.dstMethod.isPresent()) {
                var src = method.srcMethod.get();
                var dst = method.dstMethod.get();

                if (src.equals(dst)) {
                    sb.append(
                            "[![f!](https://img.shields.io/badge/modified-orange?style=flat-square)]()");
                } else {
                    sb.append(
                            "[![f!](https://img.shields.io/badge/moved-blue?style=flat-square)]()");
                }
            } else {
                sb.append(
                        "[![f!](https://img.shields.io/badge/deleted-red?style=flat-square)]()");
            }

            sb.append(String.format(
                    "[![f!](https://img.shields.io/static/v1?label=%s&message=%s()&color=informational&style=flat-square)]()",
                    method.getSrcMethod().clazzName.substring(1).replace("/", ".").replace("_", "\\_"),
                    method.getSrcMethod().methodName.replace("_", "\\_")));

            if (match.contains(method.getSrcMethod())) {
                list.add(String.format("<li>%s%s</li>", sb.toString(),
                        formatOperations(change.getValue())));
            } else {

                var t = String.format("[![f!](https://img.shields.io/static/v1?label=%s&message=AST&nbsp;changes&color=orange&style=flat-square)]()", change.getValue().size());
                special.add(String.format("<li>%s%s</li>", sb.toString(), t));
            }

        });

        list.stream().forEach(s -> report.append(s));
        special.stream().forEach(s -> report.append(s));


        report.append("</ul>");

        return report.toString();
    }

    private static CtElement getExecutableParentNode(CtElement child) {
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

    private static String formatOperations(List<Operation> changes) {

        var report = new StringBuilder();
        report.append("<ul>");

        changes.stream().limit(10).forEach(op -> {

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

            report.append(String.format("<li>%s</li>", el.toString()));
        });

        if (changes.size() > 10) {
            report.append(new ItalicText(String
                    .format("<li>... %s more AST change(s)</li>", changes.size() - 10)));
        }

        report.append("</ul>");

        return report.toString();

    }


}
