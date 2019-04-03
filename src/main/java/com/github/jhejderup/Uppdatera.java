package com.github.jhejderup;


import com.github.jhejderup.data.JDKClassPath;
import com.github.jhejderup.data.MavenCoordinate;
import com.github.jhejderup.data.Namespace;
import com.github.jhejderup.data.diff.FileDiff;
import com.github.jhejderup.data.ufi.UFI;
import com.github.jhejderup.data.ufi.UniversalType;
import com.github.jhejderup.diff.ArtifactDiff;
import com.github.jhejderup.diff.ArtifactResolver;
import gumtree.spoon.diff.operations.Operation;
import spoon.reflect.declaration.*;
import spoon.reflect.reference.CtTypeReference;
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


    private static UniversalType resolveType(CtTypeReference type, Map<String,MavenCoordinate> lookup) {
        if(type.isPrimitive()) {
            return new UniversalType(
                    Optional.of(new Namespace("PrimitiveType")),
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
                .map(arg -> resolveType(arg.getType(),lookup))
                .collect(Collectors.toList());

        return new UFI(
                resolveType(((CtType) method.getParent()).getReference(),lookup),
                method.getSimpleName(),
                args.size() > 0 ? Optional.of(args): Optional.empty(),
                resolveType(method.getType(),lookup)
        );
    }



    public static void main(String[] args)
            throws Exception {
        MavenCoordinate coord = new MavenCoordinate("com.squareup.okhttp3", "okhttp", "3.13.1");
        Map<String,MavenCoordinate> lookup = buildClassLookupTable(coord);

        ArtifactDiff
                .diff(new MavenCoordinate("com.squareup.okhttp3", "okhttp", "3.13.1"),
                        new MavenCoordinate("com.squareup.okhttp3", "okhttp", "3.14.0"))
                .forEach(diff -> {
                    if(diff.methodDiffs.isPresent()){


                        if(diff.fileDiff.type != FileDiff.Change.ADDITION && diff.fileDiff.type != FileDiff.Change.COPY){
                            Map<CtExecutable, List<Operation>> map = diff.methodDiffs.get();
                            map.keySet().stream()
                                    .map(m -> convertToUFI(m, lookup))
                                    .forEach(System.out::println);
                        }
                    }
                });


//        File[] artifacts = Maven.resolver().resolve("com.squareup.okhttp3:okcurl:3.14.0").withTransitivity().asFile();
//        List<File> arts = Arrays.asList(artifacts);
//        ArrayList<File> artlst = new ArrayList<>(arts);
//        List<String> jars = artlst.stream().map(s -> s.getAbsolutePath()).collect(Collectors.toList());
//
//        String classpath = String.join(":", new ArrayList<>(jars));
//
//        WalaCallgraphConstructor.build(classpath);


    }


}
