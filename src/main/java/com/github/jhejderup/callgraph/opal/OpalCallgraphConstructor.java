package com.github.jhejderup.callgraph.opal;

import com.github.jhejderup.callgraph.CallgraphConstructor;
import com.github.jhejderup.callgraph.CallgraphException;
import com.github.jhejderup.callgraph.ResolvedCall;
import com.github.jhejderup.callgraph.ResolvedMethod;
import com.google.common.collect.Lists;
import org.opalj.br.ClassFile;
import org.opalj.br.Method;
import org.opalj.br.analyses.Project;
import org.opalj.br.instructions.INVOKEINTERFACE;
import org.opalj.br.instructions.INVOKESPECIAL;
import org.opalj.br.instructions.INVOKESTATIC;
import org.opalj.br.instructions.INVOKEVIRTUAL;
import org.opalj.br.reader.Java8Framework$;
import org.opalj.br.reader.Java9Framework$;
import scala.Tuple2;
import scala.collection.JavaConverters;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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

        var projectSources = Java8Framework$.MODULE$.AllClassFiles(JavaConverters.collectionAsScalaIterable(projectClassFiles), Java9Framework$.MODULE$.defaultExceptionHandler());
        var librarySources = Java8Framework$.MODULE$.AllClassFiles(JavaConverters.collectionAsScalaIterable(libraryClassFiles), Java9Framework$.MODULE$.defaultExceptionHandler());

        project = Project.apply(
                JavaConverters.asScalaSet(JavaConverters.asJavaCollection(projectSources.toList()).stream().map(t -> new Tuple2<ClassFile, URL>((ClassFile) t._1, t._2)).collect(Collectors.toSet())).toTraversable(),
                JavaConverters.asScalaSet(JavaConverters.asJavaCollection(librarySources.toList()).stream().map(t -> new Tuple2<ClassFile, URL>((ClassFile) t._1, t._2)).collect(Collectors.toSet())).toTraversable(),
                false);

        // Collect entryPoints
        var entryPoints = JavaConverters.asJavaCollection(project.allProjectClassFiles().toStream().flatten(ClassFile::methodsWithBody));

        Stack<Method> workList = new Stack<>();
        workList.addAll(entryPoints);

        var result = new ArrayList<ResolvedCall>();

        var visited = new HashSet<Method>();

        while (!workList.isEmpty()) {
            var srcMtd = workList.pop();
            Set<Method> callTargets = resolveDirectMethodCallTargets(srcMtd);

            callTargets.forEach(tgtMtd -> {
                if (!visited.contains(tgtMtd)) {
                    visited.add(tgtMtd);
                    workList.add(tgtMtd);

                    var resSrc = new OpalResolvedMethod(srcMtd, project);
                    var resTgt = new OpalResolvedMethod(tgtMtd, project);

                    var call = new ResolvedCall(resSrc, resTgt);

                    result.add(call);
                }
            });
        }

        return result;
    }

    private Set<Method> resolveDirectMethodCallTargets(Method method) {
        Set<Method> result = new HashSet<>();

        if (method.body().isEmpty()) {
            return result;
        }

        for (var i : method.body().get().instructions()) {
            if (i != null && i.isInvocationInstruction()) {
                if (i instanceof INVOKEVIRTUAL) {
                    var invoke = (INVOKEVIRTUAL) i;

                    var targets = project.resolveAllMethodReferences(invoke.declaringClass(), invoke.name(), invoke.methodDescriptor());

                    result.addAll(JavaConverters.setAsJavaSet(targets));
                } else if (i instanceof INVOKEINTERFACE) {
                    var invoke = (INVOKEINTERFACE) i;

                    var targets = project.interfaceCall(method.declaringClassFile().thisType(), invoke.name(), invoke.methodDescriptor());

                    result.addAll(JavaConverters.setAsJavaSet(targets));
                } else if (i instanceof INVOKESTATIC) {
                    var invoke = (INVOKESTATIC) i;

                    var target = project.staticCall(invoke);

                    if (target.hasValue()) {
                        result.add(target.value());
                    }
                } else if (i instanceof INVOKESPECIAL) {
                    var invoke = (INVOKESPECIAL) i;

                    var target = project.specialCall(invoke.declaringClass().asObjectType(), invoke);

                    if (target.hasValue()) {
                        result.add(target.value());
                    }
                }
            }
        }
        return result;
    }
}
