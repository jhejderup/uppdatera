package com.github.jhejderup.generator;

import com.github.jhejderup.data.ModuleClasspath;
import com.github.jhejderup.data.callgraph.ResolvedCall;
import com.github.jhejderup.data.callgraph.WalaCallGraph;
import com.ibm.wala.classLoader.*;
import com.ibm.wala.ipa.callgraph.*;
import com.ibm.wala.ipa.callgraph.impl.DefaultEntrypoint;
import com.ibm.wala.ipa.callgraph.impl.Util;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.cha.ClassHierarchyFactory;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.util.config.AnalysisScopeReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
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


    private static Predicate<CGNode> uppdateraLoaderFilter =
            node -> isApplication(node.getMethod().getDeclaringClass()) || isExtension(node.getMethod().getDeclaringClass());

    public static WalaCallGraph build(ModuleClasspath analysisClasspath) {

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
            var cha = ClassHierarchyFactory.makeWithPhantom(scope);

            //4. Specify Entrypoints -> all non-primordial public entrypoints (also with declared parameters, not sub-types)
            var entryPoints = makeEntryPoints(cha);

            //5. Encapsulates various analysis options
            var options = new AnalysisOptions(scope, entryPoints);
            var cache = new AnalysisCacheImpl();

            //6 Build the call graph
            var builder = Util.makeRTABuilder(options, cache, cha, scope);
            var callGraph = builder.makeCallGraph(options, null);
            return new WalaCallGraph(callGraph, analysisClasspath);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static List<ResolvedCall> resolveCalls(CallGraph cg) {
        Iterable<CGNode> cgNodes = () -> cg.iterator();
        var calls = StreamSupport
                .stream(cgNodes.spliterator(), false)
                .filter(uppdateraLoaderFilter)
                .flatMap(node -> {
                    Iterable<CallSiteReference> callSites = () -> node.iterateCallSites();
                    return StreamSupport
                            .stream(callSites.spliterator(), false)
                            .flatMap(site -> {
                                MethodReference ref = site.getDeclaredTarget();
                                if(site.isDispatch()){
                                    return cg.getClassHierarchy()
                                            .getPossibleTargets(ref)
                                            .stream()
                                            .map(target -> new ResolvedCall(
                                                    node.getMethod(),
                                                    site.getInvocationCode(),
                                                    target));
                                } else {
                                    IMethod target = cg.getClassHierarchy().resolveMethod(ref);
                                    if(target != null){
                                    return Stream.of(new ResolvedCall(
                                            node.getMethod(),
                                            site.getInvocationCode(),
                                            target));
                                    } else {
                                        return null;
                                    }
                                }

                            });
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        return calls;
    }

    ///
    /// Fetch JAR File
    ///
    public static String fetchJarFile(IClass klass) {
        var shrikeKlass = (ShrikeClass) klass;
        var moduleEntry = (JarFileEntry) shrikeKlass.getModuleEntry();
        var jarFile = moduleEntry.getJarFile();
        var jarPath = jarFile.getName();
        return jarPath;
    }

    private static ArrayList<Entrypoint> makeEntryPoints(ClassHierarchy cha) {
        Iterable<IClass> classes = () -> cha.iterator();
        var entryPoints = StreamSupport.stream(classes.spliterator(), false)
                .filter(WalaCallgraphConstructor::skipInterface)
                .flatMap(klass -> klass.getAllMethods().parallelStream())
                .filter(WalaCallgraphConstructor::skipAbstractMethods)
                .map(m -> new DefaultEntrypoint(m, cha))
                .collect(Collectors.toList());
        return new ArrayList<>(entryPoints);
    }

    ///
    /// Helper functions
    ///
    private static boolean skipInterface(IClass klass) {
        return isApplication(klass) && !klass.isInterface();

    }

    private static boolean skipAbstractMethods(IMethod method) {
        return isApplication(method.getDeclaringClass()) && !method.isAbstract();
    }

    public static Boolean isApplication(IClass klass) {
        return klass.getClassLoader().getReference().equals(ClassLoaderReference.Application);
    }

    public static Boolean isExtension(IClass klass) {
        return klass.getClassLoader().getReference().equals(ClassLoaderReference.Extension);
    }

    public static boolean isUppdateraScope(IMethod method) {
        return (isApplication(method.getDeclaringClass()) || isExtension(method.getDeclaringClass()));
}

}
