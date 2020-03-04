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
package com.github.jhejderup.callgraph.wala;

import com.github.jhejderup.callgraph.CallgraphConstructor;
import com.github.jhejderup.callgraph.ResolvedCall;
import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.AnalysisCacheImpl;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.callgraph.impl.DefaultEntrypoint;
import com.ibm.wala.ipa.callgraph.impl.Util;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.cha.ClassHierarchyFactory;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.util.config.AnalysisScopeReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public final class WalaCallgraphConstructor implements CallgraphConstructor {

    private static Logger logger = LoggerFactory
            .getLogger(WalaCallgraphConstructor.class);

    @Override
    public List<ResolvedCall> build(String projectClasspath, String depzClasspath) {
        try {

            logger.info("Building call graph with project classpath: {}", projectClasspath);
            logger.info("Building call graph with depz classpath: {}", depzClasspath);

            //1. Fetch exclusion file
            var classLoader = WalaCallgraphConstructor.class.getClassLoader();
            var exclusionFile = new File(
                    classLoader.getResource("Java60RegressionExclusions.txt").getFile());

            //2. Set the analysis scope
            var scope = AnalysisScopeReader
                    .makeJavaBinaryAnalysisScope(projectClasspath, exclusionFile);
            //Add dependency classes under the extension scope
            AnalysisScopeReader.addClassPathToScope(depzClasspath, scope,
                    scope.getLoader(AnalysisScope.EXTENSION));

            //3. Class Hierarchy for name resolution -> missing superclasses are replaced by the ClassHierarchy root,
            //   i.e. java.lang.Object
            var classHierarchy = ClassHierarchyFactory.makeWithRoot(scope);

            logger.info("[Uppdatera] Creating entry points...");

            //4. Both Private/Public functions are entry-points
            var entryPoints = makeEntryPoints(scope, classHierarchy);

            //5. Encapsulates various analysis options
            var options = new AnalysisOptions(scope, entryPoints);
            var cache = new AnalysisCacheImpl();

            logger.info("[Uppdatera] Building callgraph...");

            //6 Build the call graph
            var builder = Util.makeRTABuilder(options, cache, classHierarchy, scope);
            var cg = builder.makeCallGraph(options, null);

            logger.info("[Uppdatera] Resolving call targets...");

            return resolveCallTargets(cg);

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private List<ResolvedCall> resolveCallTargets(CallGraph cg) {
        //Get all entrypoints and turn into a J8 Stream
        var callSites = itrToStream(
                cg.getFakeRootNode().iterateCallSites());

        //Place initial nodes in a work list
        var workList = callSites.map(CallSiteReference::getDeclaredTarget)
                .collect(Collectors.toCollection(Stack::new));

        var visited = new HashSet<MethodReference>();
        var calls = new ArrayList<ResolvedCall>();

        while (!workList.empty()) {
            var srcMref = workList.pop();
            //Resolve ref to impl
            var resolveMethod = cg.getClassHierarchy().resolveMethod(srcMref);

            cg.getNodes(srcMref).stream().forEach(cgNode -> {
                itrToStream(cgNode.iterateCallSites())
                        .flatMap(cs -> cg.getPossibleTargets(cgNode, cs).stream()) // get concrete call targets
                        .map(n -> n.getMethod().getReference()).forEach(csMref -> {
                    if (!visited.contains(csMref)) {
                        workList.add(csMref);
                        visited.add(csMref);
                        if (resolveMethod != null) {
                            ResolvedCall call = new ResolvedCall(new WalaResolvedMethod(resolveMethod.getReference()), new WalaResolvedMethod(csMref));
//                        logger.info(call.toString());
                            calls.add(call);
                        }
                    }

                });
            });
        }
        return calls;
    }

    private <T> Stream<T> itrToStream(Iterator<T> itr) {
        Iterable<T> iterable = () -> itr;
        return StreamSupport.stream(iterable.spliterator(), false);
    }

    private ArrayList<Entrypoint> makeEntryPoints(AnalysisScope scope, ClassHierarchy classHierarchy) {
        Iterable<IClass> classes = () -> classHierarchy.iterator();
        var entryPoints = StreamSupport.stream(classes.spliterator(), false)
                .filter(clazz -> skipInterface(scope, clazz))
                .flatMap(clazz -> clazz.getDeclaredMethods().parallelStream())
                .filter(m -> skipAbstractMethod(scope, m))
                .map(m -> new DefaultEntrypoint(m, classHierarchy)).collect(Collectors.toList());
        return new ArrayList<>(entryPoints);
    }

    private boolean skipInterface(AnalysisScope scope, IClass klass) {
        return !klass.isInterface() && isApplication(scope, klass);
    }

    private boolean skipAbstractMethod(AnalysisScope scope,
                                       IMethod method) {
        return !method.isAbstract() && isApplication(scope,
                method.getDeclaringClass());
    }

    public Boolean isApplication(AnalysisScope scope, IClass klass) {
        return scope.getApplicationLoader()
                .equals(klass.getClassLoader().getReference());
    }

    private Boolean isExtension(AnalysisScope scope, IClass klass) {
        return scope.getExtensionLoader()
                .equals(klass.getClassLoader().getReference());
    }

}
