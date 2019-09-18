package com.github.jhejderup.data.callgraph;

import com.github.jhejderup.data.ModuleClasspath;
import com.github.jhejderup.data.type.*;
import com.github.jhejderup.data.ufi.UFI;
import com.github.jhejderup.data.ufi.UniversalArrayType;
import com.github.jhejderup.data.ufi.UniversalFunctionIdentifier;
import com.github.jhejderup.data.ufi.UniversalType;
import com.github.jhejderup.generator.WalaCallgraphConstructor;
import com.ibm.wala.classLoader.ArrayClass;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.types.TypeName;
import com.ibm.wala.types.TypeReference;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toMap;

public final class WalaCallGraph implements Serializable, UniversalFunctionIdentifier<IMethod> {

    public final CallGraph rawGraph;
    public final ModuleClasspath analyzedClasspath;
    public final Map<String, MavenResolvedCoordinate> jarToCoordinate;
    private final Map<IMethod, List<IMethod>> reverseGraph;


    public WalaCallGraph(CallGraph rawGraph, ModuleClasspath analyzedClasspath) {
        this.rawGraph = rawGraph;
        this.analyzedClasspath = analyzedClasspath;
        this.jarToCoordinate = analyzedClasspath
                .getCompleteClasspath()
                .stream()
                .collect(toMap(c -> c.jarPath.toString(), Function.identity()));
        this.reverseGraph = WalaCallgraphConstructor
                .makeCHA(rawGraph)
                .stream()
                .collect(Collectors.toMap(call -> call.target,
                        call -> {
                            var lst = new ArrayList<IMethod>();
                            lst.add(call.source);
                            return lst;
                        }, (oldLst, newLst) -> {
                            oldLst.addAll(newLst);
                            return oldLst;
                        }));
    }

    public void walk(IMethod method) {
        if (this.reverseGraph.containsKey(method)) {
            System.out.println(method);
            this.reverseGraph.get(method).stream().forEach(this::walk);
        }
    }

    private Optional<Namespace> getGlobalNamespace(IClass klass) {
        if (klass instanceof ArrayClass) {
            return Optional.of(JDKPackage.getInstance());
        }

        try {
            String jarFile = WalaCallgraphConstructor.fetchJarFile(klass);
            if (!jarFile.endsWith("rt.jar")) {
                return Optional.of(this.jarToCoordinate.get(jarFile));
            } else {
                return Optional.of(JDKPackage.getInstance());
            }
        } catch (Exception e) {
            e.printStackTrace();
            return Optional.empty();
        }
    }

    private UniversalType resolveArrayType(TypeReference tyref) {
        assert tyref.isArrayType();

        TypeName ty = tyref.getName().parseForArrayElementName();
        String ref = tyref.getName().toString();
        int countBrackets = ref.length() - ref.replace("[", "").length();

        if (ty.isPrimitiveType()) {
            return new UniversalArrayType(Optional.of(JDKPackage.getInstance()), JavaPrimitive.of(tyref), countBrackets);
        } else if (ty.isClassType()) {
            IClass klass = this.rawGraph.getClassHierarchy().lookupClass(tyref);
            Namespace inner = klass != null ?
                    new JavaPackage(((klass.getName().toString()).substring(1)).substring(1).split("/")) :
                    new JavaPackage("_unknownType", tyref.getName().toString());
            Optional<Namespace> outer = klass != null ? getGlobalNamespace(klass) : Optional.empty();
            return new UniversalArrayType(outer, inner, countBrackets);
        } else {
            return new UniversalType(Optional.empty(), new JavaPackage("unknown"));
        }
    }


    private UniversalType resolveTypeRef(TypeReference tyref) {
        if (tyref.isPrimitiveType()) {
            return new UniversalType(Optional.of(JDKPackage.getInstance()), JavaPrimitive.of(tyref));
        } else if (tyref.isClassType()) {
            IClass klass = this.rawGraph.getClassHierarchy().lookupClass(tyref);
            Namespace inner = klass != null ?
                    new JavaPackage((klass.getName().toString()).substring(1).split("/")) :
                    new JavaPackage("_unknownType", tyref.getName().toString());
            Optional<Namespace> outer = klass != null ? getGlobalNamespace(klass) : Optional.empty();
            return new UniversalType(outer, inner);
        } else if (tyref.isArrayType()) {
            return resolveArrayType(tyref);
        } else {
            return new UniversalType(Optional.empty(), new JavaPackage("unknown"));
        }
    }

    @Override
    public UFI convertToUFI(IMethod item) {
        //1. create resolved path
        var outer = getGlobalNamespace(item.getDeclaringClass());
        var inner = new JavaPackage((item.getDeclaringClass().getName().toString()).substring(1).split("/"));
        var path = new UniversalType(outer, inner);
        //2. extract methodname
        var methodName = item.getName().toString();
        //3. resolve return type
        UniversalType returnType = item.isInit() ? resolveTypeRef(item.getParameterType(0))
                : resolveTypeRef(item.getReturnType());
        //4. resolve parameter types
        Optional<List<UniversalType>> args = item.getNumberOfParameters() > 0 ?
                Optional.of(IntStream.range(1, item.getNumberOfParameters())
                        .mapToObj(i -> resolveTypeRef(item.getParameterType(i)))
                        .collect(Collectors.toList())) : Optional.empty();

        return new UFI(path, methodName, args, returnType);

    }

    @Override
    public Map<UFI, IMethod> mappings() {
        return WalaCallgraphConstructor
                .makeCHA(this.rawGraph)
                .stream()
                .flatMap(call -> Stream.of(call.source, call.target))
                .collect(toMap(c -> convertToUFI(c), Function.identity(), (v1, v2) -> v1));
    }
}