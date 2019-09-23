package com.github.jhejderup.generator;

import com.github.jhejderup.data.ModuleClasspath;
import com.github.jhejderup.data.callgraph.ResolvedCall;
import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.*;
import com.ibm.wala.ipa.callgraph.impl.DefaultEntrypoint;
import com.ibm.wala.ipa.callgraph.impl.Util;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.cha.ClassHierarchyFactory;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.util.config.AnalysisScopeReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static java.util.stream.Collectors.joining;

public final class WalaCallgraphConstructor {


    private static Logger logger = LoggerFactory.getLogger(WalaCallgraphConstructor.class);

    //A filter that accepts WALA objects that "belong" to the application loader.
    private static Predicate<CGNode> applicationLoaderFilter =
            node -> isApplication(node.getMethod().getDeclaringClass());

    private static Predicate<CGNode> extensionLoaderFilter =
            node -> isExtension(node.getMethod().getDeclaringClass());


    private static Predicate<CGNode> uppdateraLoaderFilter =
            node -> applicationLoaderFilter.test(node) ||
                    extensionLoaderFilter.test(node);

    public static CallGraph build(ModuleClasspath analysisClasspath) {

        try {
            var classpath = analysisClasspath
                    .getCompleteClasspath()
                    .stream()
                    .map(c -> c.jarPath.toString()).collect(joining(":"));

            logger.info("Building call graph with classpath: {}", classpath);
            //1. Fetch exclusion file
            var classLoader = WalaCallgraphConstructor.class.getClassLoader();
            var exclusionFile = new File(classLoader.getResource("Java60RegressionExclusions.txt").getFile());

            var appJAR = analysisClasspath.project.jarPath.toString();

            //2. Set the analysis scope
            var scope = AnalysisScopeReader.makeJavaBinaryAnalysisScope(appJAR, exclusionFile);

            if (analysisClasspath.dependencies.isPresent()) {

                var depzJARpath = analysisClasspath.dependencies.get().stream()
                        .map(c -> c.jarPath.toString()).collect(joining(":"));
                AnalysisScopeReader.addClassPathToScope(depzJARpath, scope, scope.getLoader(AnalysisScope.EXTENSION));
            }

            //3. Class Hierarchy for name resolution -> missing superclasses are replaced by the ClassHierarchy root,
            //   i.e. java.lang.Object
            var cha = ClassHierarchyFactory.makeWithRoot(scope);

            //4. Both Private/Public functions are entry-points
            var entryPoints = makeEntryPoints(cha);

            //5. Encapsulates various analysis options
            var options = new AnalysisOptions(scope, entryPoints);
            var cache = new AnalysisCacheImpl();

            //6 Build the call graph
            var builder = Util.makeRTABuilder(options, cache, cha, scope);
            return builder.makeCallGraph(options, null);

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
        var entryPointsStream = itrToStream(cg.getFakeRootNode().iterateCallSites());

        //Place initial nodes in a work list
        var workList = entryPointsStream
                .map(CallSiteReference::getDeclaredTarget)
                .collect(Collectors.toCollection(Stack::new));

        var visited = new HashSet<>();
        var calls = new ArrayList<ResolvedCall>();

        while (!workList.empty()) {
            var srcMref = workList.pop();
            //Resolve ref to impl
            var resolveMethod = cg.getClassHierarchy().resolveMethod(srcMref);

            cg.getNodes(srcMref)
                    .stream()
                    .forEach(cgNode -> {
                        itrToStream(cgNode.iterateCallSites())
                                .flatMap(cs -> cg.getPossibleTargets(cgNode, cs).stream())
                                .map(n -> n.getMethod().getReference())
                                .forEach(csMref -> {
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

    private static ArrayList<Entrypoint> makeEntryPoints(ClassHierarchy cha) {
        Iterable<IClass> classes = () -> cha.iterator();
        var entryPoints = StreamSupport.stream(classes.spliterator(), false)
                .filter(WalaCallgraphConstructor::skipInterface)
                .flatMap(klass -> klass.getAllMethods().parallelStream())
                .filter(WalaCallgraphConstructor::skipAbstractMethod)
                .map(m -> new DefaultEntrypoint(m, cha))
                .collect(Collectors.toList());
        return new ArrayList<>(entryPoints);
    }

    private static boolean skipInterface(IClass klass) {
        return isApplication(klass) && !klass.isInterface();
    }

    private static boolean skipAbstractMethod(IMethod method) {
        return isApplication(method.getDeclaringClass()) && !method.isAbstract();
    }

    public static Boolean isApplication(IClass klass) {
        return klass.getClassLoader().getReference().equals(ClassLoaderReference.Application);
    }

    public static Boolean isExtension(IClass klass) {
        return klass.getClassLoader().getReference().equals(ClassLoaderReference.Extension);
    }

}
