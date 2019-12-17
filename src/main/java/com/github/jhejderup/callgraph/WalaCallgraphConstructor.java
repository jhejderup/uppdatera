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
package com.github.jhejderup.callgraph;

import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.*;
import com.ibm.wala.ipa.callgraph.impl.DefaultEntrypoint;
import com.ibm.wala.ipa.callgraph.impl.Util;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.cha.ClassHierarchyFactory;
import com.ibm.wala.util.config.AnalysisScopeReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public final class WalaCallgraphConstructor {

  private static Logger logger = LoggerFactory
      .getLogger(WalaCallgraphConstructor.class);

  public static CallGraph build(String classpath_project,
      String classpath_depz) {

    try {

      logger.info("Building call graph with project classpath: {}",
          classpath_project);
      logger
          .info("Building call graph with depz classpath: {}", classpath_depz);
      //1. Fetch exclusion file
      var classLoader = WalaCallgraphConstructor.class.getClassLoader();
      var exclusionFile = new File(
          classLoader.getResource("Java60RegressionExclusions.txt").getFile());

      //2. Set the analysis scope
      var scope = AnalysisScopeReader
          .makeJavaBinaryAnalysisScope(classpath_project, exclusionFile);
      AnalysisScopeReader.addClassPathToScope(classpath_depz, scope,
          scope.getLoader(AnalysisScope.EXTENSION));

      //3. Class Hierarchy for name resolution -> missing superclasses are replaced by the ClassHierarchy root,
      //   i.e. java.lang.Object
      var cha = ClassHierarchyFactory.makeWithRoot(scope);

      //4. Both Private/Public functions are entry-points
      var entryPoints = makeEntryPoints(scope, cha);

      //5. Encapsulates various analysis options
      var options = new AnalysisOptions(scope, entryPoints);
      var cache = new AnalysisCacheImpl();

      //6 Build the call graph
      var builder = Util.makeRTABuilder(options, cache, cha, scope);
      var cg = builder.makeCallGraph(options, null);

      return cg;

    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }

  private static <T> Stream<T> itrToStream(Iterator<T> itr) {
    Iterable<T> iterable = () -> itr;
    return StreamSupport.stream(iterable.spliterator(), false);
  }

  public static List<ResolvedCall> makeCHA(CallGraph cg) {
    //Get all entrypoints and turn into a J8 Stream
    var entryPointsStream = itrToStream(
        cg.getFakeRootNode().iterateCallSites());

    //Place initial nodes in a work list
    var workList = entryPointsStream.map(CallSiteReference::getDeclaredTarget)
        .collect(Collectors.toCollection(Stack::new));

    var visited = new HashSet<>();
    var calls = new ArrayList<ResolvedCall>();

    while (!workList.empty()) {
      var srcMref = workList.pop();
      //Resolve ref to impl
      var resolveMethod = cg.getClassHierarchy().resolveMethod(srcMref);

      cg.getNodes(srcMref).stream().forEach(cgNode -> {
        itrToStream(cgNode.iterateCallSites())
            .flatMap(cs -> cg.getPossibleTargets(cgNode, cs).stream())
            .map(n -> n.getMethod().getReference()).forEach(csMref -> {
          if (!visited.contains(csMref)) {
            workList.add(csMref);
            visited.add(csMref);
          }
          if (resolveMethod != null) {
            calls.add(new ResolvedCall(resolveMethod.getReference(), csMref));
          }
        });
      });
    }
    return calls;
  }

  private static ArrayList<Entrypoint> makeEntryPoints(AnalysisScope scope,
      ClassHierarchy cha) {
    Iterable<IClass> classes = () -> cha.iterator();
    var entryPoints = StreamSupport.stream(classes.spliterator(), false)
        .filter(clazz -> skipInterface(scope, clazz))
        .flatMap(clazz -> clazz.getDeclaredMethods().parallelStream())
        .filter(m -> skipAbstractMethod(scope, m))
        .map(m -> new DefaultEntrypoint(m, cha)).collect(Collectors.toList());
    return new ArrayList<>(entryPoints);
  }

  private static boolean skipInterface(AnalysisScope scope, IClass klass) {
    return !klass.isInterface() && isApplication(scope, klass);
  }

  private static boolean skipAbstractMethod(AnalysisScope scope,
      IMethod method) {
    return !method.isAbstract() && isApplication(scope,
        method.getDeclaringClass());
  }

  public static Boolean isApplication(AnalysisScope scope, IClass klass) {
    return scope.getApplicationLoader()
        .equals(klass.getClassLoader().getReference());
  }

  public static Boolean isExtension(AnalysisScope scope, IClass klass) {
    return scope.getExtensionLoader()
        .equals(klass.getClassLoader().getReference());
  }

}
