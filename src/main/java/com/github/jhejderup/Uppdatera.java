package com.github.jhejderup;


import com.github.jhejderup.connectors.EmbeddedGradle;
import com.github.jhejderup.data.*;
import com.github.jhejderup.data.diff.FileDiff;
import com.github.jhejderup.data.ufi.UFI;
import com.github.jhejderup.data.ufi.UniversalType;
import com.github.jhejderup.diff.ArtifactDiff;
import com.github.jhejderup.diff.ArtifactResolver;
import com.github.jhejderup.wala.WalaCallgraphConstructor;
import com.ibm.wala.classLoader.IMethod;
import gumtree.spoon.diff.operations.Operation;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import spoon.reflect.declaration.*;
import spoon.reflect.reference.CtTypeReference;

import java.io.File;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class Uppdatera {

    public static <T> Predicate<T> distinctByKey(Function<? super T, ?> keyExtractor) {
        Set<Object> seen = ConcurrentHashMap.newKeySet();
        return t -> seen.add(keyExtractor.apply(t));
    }


    public static Map<String, MavenCoordinate> buildClassLookupTable(MavenCoordinate coordinate){
        return ArtifactResolver
                .resolveDependencyTree(coordinate)
                .filter(distinctByKey(ar -> ar.asFile().toString()))
                .flatMap(ArtifactResolver::getClasses)
                .flatMap(m -> m.entrySet().stream())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }


    private static UniversalType resolveSpoonType(CtTypeReference type, Map<String,MavenCoordinate> lookup) {
        if(type.isPrimitive()) {
            return new UniversalType(
                    Optional.of(new Namespace("JavaPrimitive")),
                    new Namespace(type.getSimpleName()));
        } else {
            if(type.getQualifiedName().startsWith("java")){
                return new UniversalType(
                        Optional.of(new JDKClassPath()),
                        new Namespace(type.getQualifiedName().split("\\.")));
            } else {
                if(lookup.containsKey(type.getQualifiedName())){
                    return new UniversalType(
                            Optional.of(lookup.get(type.getQualifiedName())),
                            new Namespace(type.getQualifiedName().split("\\.")));

                } else {
                    return new UniversalType(
                            Optional.empty(),
                            new Namespace(type.getQualifiedName()));
                }
            }
        }
    }


    public static UFI convertToUFI(CtExecutable method, Map<String,MavenCoordinate> lookup) {

        List<UniversalType> args =
                Arrays.stream(method.getParameters().toArray())
                .map(CtParameter.class::cast)
                .map(arg -> resolveSpoonType(arg.getType(),lookup))
                .collect(Collectors.toList());


        return new UFI(
                resolveSpoonType(((CtType) method.getParent()).getReference(),lookup),
                method.getSimpleName(),
                args.size() > 0 ? Optional.of(args): Optional.empty(),
                resolveSpoonType(method.getType(),lookup)
        );
    }



    public static void main(String[] args)
            throws Exception {

        String[] leftArtifact = args[1].split(":");
        String[] rightArtifact = args[2].split(":");

        MavenCoordinate coordLeft = new MavenCoordinate(leftArtifact[0], leftArtifact[1], leftArtifact[2]);
        MavenCoordinate coordRight = new MavenCoordinate(rightArtifact[0], rightArtifact[1], rightArtifact[2]);
        Map<String,MavenCoordinate> lookup = buildClassLookupTable(coordLeft);

        List<UppdateraCoordinate> clientClassPath = EmbeddedGradle.getClasspath(Paths.get(args[0]));
        String classpath = clientClassPath
                .stream()
                .map(d -> d.jarFile)
                .map(Objects::toString)
                .collect(Collectors.joining(","));

        List<ResolvedCall> cg = WalaCallgraphConstructor.build(classpath);
        Map<UFI, IMethod> cg_table = WalaCallgraphConstructor.mapping(cg);

        ArtifactDiff
                .diff(coordLeft, coordRight)
                .forEach(diff -> {
                    if(diff.fileDiff.type != FileDiff.Change.ADDITION && diff.fileDiff.type != FileDiff.Change.COPY){
                        if(diff.methodDiffs.isPresent()) {
                            Map<CtExecutable,List<Operation>> methods = diff.methodDiffs.get();

                            methods.keySet()
                                    .stream()
                                    .map(m -> convertToUFI(m,lookup))
                                    .forEach(UFI -> {
                                        if(cg_table.containsKey(UFI)){
                                            System.out.println("IN CG!");
                                            System.out.println(cg_table.get(UFI));
                                        } else {
                                            System.out.println("NOT IN CG!");
                                            System.out.println(UFI);
                                        }
                                    });
                        }
                    }
                });




    }


}
