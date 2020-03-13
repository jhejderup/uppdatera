package com.github.jhejderup.callgraph.opal;

import static scala.collection.JavaConverters.asJavaCollection;
import static scala.collection.JavaConverters.asJavaIterable;
import static scala.collection.JavaConverters.asScalaSet;
import static scala.collection.JavaConverters.collectionAsScalaIterable;

import com.github.jhejderup.callgraph.CallgraphConstructor;
import com.github.jhejderup.callgraph.CallgraphException;
import com.github.jhejderup.callgraph.ResolvedCall;
import com.google.common.collect.Lists;
import com.typesafe.config.ConfigFactory;
import org.opalj.ai.analyses.cg.CHACallGraphAlgorithmConfiguration;
import org.opalj.ai.analyses.cg.CallGraphFactory;
import org.opalj.ai.analyses.cg.ComputedCallGraph;
import org.opalj.bi.reader.ClassFileReader;
import org.opalj.br.ClassFile;
import org.opalj.br.Method;
import org.opalj.br.analyses.Project;
import org.opalj.br.reader.Java9Framework$;
import scala.Tuple2;
import scala.collection.Iterator;
import scala.collection.JavaConverters;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Stack;
import java.util.stream.Collectors;

public final class OpalCallgraphConstructor implements CallgraphConstructor {

    private Project<URL> project;

    @Override
    public List<ResolvedCall> build(String projectClassPath, String dependencyClassPath) throws CallgraphException {
        List<File> projectClassFiles = Lists.newArrayList(new File(projectClassPath));

        String[] depNames = dependencyClassPath.split(":");
        List<File> libraryClassFiles = new ArrayList<>();

        for (String depName : depNames) {
            libraryClassFiles.add(new File(depName));
        }

        var exceptionHandler = ClassFileReader.defaultExceptionHandler();
        var logContext = Java9Framework$.MODULE$.logContext();
        var config = ConfigFactory.load("reference2.conf");
        var cfReader = Project.JavaClassFileReader(logContext, config);

        var projectSources = cfReader.AllClassFiles(collectionAsScalaIterable(projectClassFiles), exceptionHandler);
        var librarySources = cfReader.AllClassFiles(collectionAsScalaIterable(libraryClassFiles), exceptionHandler);

//        var projectSources = Java8Framework$.MODULE$.AllClassFiles(JavaConverters.collectionAsScalaIterable(projectClassFiles), exceptionHandler);
//        var librarySources = Java8Framework$.MODULE$.AllClassFiles(JavaConverters.collectionAsScalaIterable(libraryClassFiles), exceptionHandler);

//        project = Project.apply(projectSources, librarySources, true);

        project = Project.apply(
                asScalaSet(asJavaCollection(projectSources.toList()).stream().map(t -> new Tuple2<ClassFile, URL>((ClassFile) t._1, t._2)).collect(Collectors.toSet())).toTraversable(),
                asScalaSet(asJavaCollection(librarySources.toList()).stream().map(t -> new Tuple2<ClassFile, URL>((ClassFile) t._1, t._2)).collect(Collectors.toSet())).toTraversable(),
                false,
                new scala.collection.immutable.HashSet<>(),
                (lc, ex) -> null,
                config,
                logContext
        );

        var entryPoints = findEntryPoints(asJavaIterable(project.allProjectClassFiles().toStream().flatten(cf -> (Iterator<Method>) cf.methodsWithBody().map(t -> t._1))));

        var cg = CallGraphFactory.create(project, () -> entryPoints, new CHACallGraphAlgorithmConfiguration(project, true));

        return resolveCallTargets(cg);
    }

    private List<ResolvedCall> resolveCallTargets(ComputedCallGraph cg) {
        Stack<Method> workList = new Stack<>();
        workList.addAll(asJavaCollection(cg.entryPoints().apply()));

        var result = new ArrayList<ResolvedCall>();

        var visited = new HashSet<Method>();

        while (!workList.isEmpty()) {
            var source = workList.pop();

            final var targetsMap = cg.callGraph().calls((source));
            if (targetsMap != null && !targetsMap.isEmpty()) {
                for (final var keyValue : JavaConverters.asJavaIterable(targetsMap)) {
                    for (final var target : JavaConverters.asJavaIterable(keyValue._2())) {
                        if (!visited.contains(target)) {
                            visited.add(target);
                            workList.add(target);
                            var resSrc = new OpalResolvedMethod(source, project);
                            var resTgt = new OpalResolvedMethod(target, project);

                            var call = new ResolvedCall(resSrc, resTgt);

                            result.add(call);
                        }
                    }
                }
            }
        }
        return result;
    }

    /**
     * Finds non abstract and non private methods of the artifact as entrypoints for call graph
     * generation.
     *
     * @param methods are all of the {@link org.opalj.br.Method} in an OPAL-loaded project.
     * @return An {@link Iterable} of entrypoints to be consumed by scala-written OPAL.
     */
    public static scala.collection.Iterable<Method> findEntryPoints(final Iterable<Method> methods) {
        final List<org.opalj.br.Method> result = new ArrayList<>();

        for (final var method : methods) {
            if (!(method.isAbstract()) && !(method.isPrivate())) {
                result.add(method);
            }
        }
        return JavaConverters.collectionAsScalaIterable(result);

    }

//    private Set<Method> resolveDirectMethodCallTargets(Method method) {
//        Set<Method> result = new HashSet<>();
//
//        if (method.body().isEmpty()) {
//            return result;
//        }
//
//        for (var i : method.body().get().instructions()) {
//            if (i != null && i.isMethodInvocationInstruction()) {
//                MethodInvocationInstruction invoke = i.asMethodInvocationInstruction();
//
//                var targets = project.resolveAllMethodReferences(invoke.declaringClass(), invoke.name(), invoke.methodDescriptor());
//
//                result.addAll(setAsJavaSet(targets));
//            }
//        }
//        return result;
//    }

}
