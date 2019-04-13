package com.github.jhejderup;

import com.github.jhejderup.data.type.*;
import com.github.jhejderup.data.ufi.ArrayType;
import com.github.jhejderup.data.ufi.UFI;
import com.github.jhejderup.data.ufi.UniversalFunctionIdentifier;
import com.github.jhejderup.data.ufi.UniversalType;
import com.github.jhejderup.resolver.ArtifactResolver;
import spoon.reflect.declaration.CtExecutable;
import spoon.reflect.declaration.CtParameter;
import spoon.reflect.declaration.CtType;
import spoon.reflect.reference.CtArrayTypeReference;
import spoon.reflect.reference.CtTypeReference;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toMap;

public final class SpoonUFIAdapter implements UniversalFunctionIdentifier<CtExecutable>, Serializable {

    public final Map<String, MavenResolvedCoordinate> classToCoordinate;


    private SpoonUFIAdapter(MavenCoordinate coordinate) {
        this.classToCoordinate = buildClassLookupTable(coordinate);
    }

    public static SpoonUFIAdapter withTransitive(MavenCoordinate coordinate) {
        return new SpoonUFIAdapter(coordinate);
    }


    private static <T> Predicate<T> distinctByKey(Function<? super T, ?> keyExtractor) {
        Set<Object> seen = ConcurrentHashMap.newKeySet();
        return t -> seen.add(keyExtractor.apply(t));
    }

    private static Map<String, MavenResolvedCoordinate> buildClassLookupTable(MavenCoordinate coordinate) {
        return ArtifactResolver
                .resolveDependencyTree(coordinate)
                .filter(distinctByKey(ar -> ar.asFile().toString()))
                .flatMap(ArtifactResolver::getClasses)
                .flatMap(m -> m.entrySet().stream())
                .collect(toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private UniversalType resolveSpoonType(CtTypeReference type) {

        if (type.isPrimitive()) {

            if (type instanceof CtArrayTypeReference) {
                return new ArrayType(
                        Optional.of(JDKPackage.getInstance()),
                        JavaPrimitive.valueOf(type.getSimpleName().toUpperCase()),
                        ((CtArrayTypeReference) type).getDimensionCount()  // number of '[]'
                );
            }

            return new UniversalType(
                    Optional.of(JDKPackage.getInstance()),
                    JavaPrimitive.valueOf(type.getSimpleName().toUpperCase()));
        } else {
            if (type.getQualifiedName().startsWith("java")) {

                if (type instanceof CtArrayTypeReference) {
                    return new ArrayType(
                            Optional.of(JDKPackage.getInstance()),
                            new JavaPackage(type.getQualifiedName().split("\\.")),
                            ((CtArrayTypeReference) type).getDimensionCount()  // number of '[]'
                    );
                }

                return new UniversalType(
                        Optional.of(JDKPackage.getInstance()),
                        new JavaPackage(type.getQualifiedName().split("\\.")));
            } else {
                if (this.classToCoordinate.containsKey(type.getQualifiedName())) {
                    if (type instanceof CtArrayTypeReference) {
                        return new ArrayType(
                                Optional.of(this.classToCoordinate.get(type.getQualifiedName())),
                                new JavaPackage(type.getQualifiedName().split("\\.")),
                                ((CtArrayTypeReference) type).getDimensionCount()  // number of '[]'
                        );
                    }
                    return new UniversalType(
                            Optional.of(this.classToCoordinate.get(type.getQualifiedName())),
                            new JavaPackage(type.getQualifiedName().split("\\.")));

                } else {
                    return new UniversalType(
                            Optional.empty(),
                            new JavaPackage(type.getQualifiedName()));
                }
            }
        }
    }

    @Override
    public UFI convertToUFI(CtExecutable item) {
        List<UniversalType> args =
                Arrays.stream(item.getParameters().toArray())
                        .map(CtParameter.class::cast)
                        .map(arg -> resolveSpoonType(arg.getType()))
                        .collect(Collectors.toList());

        return new UFI(
                resolveSpoonType(((CtType) item.getParent()).getReference()),
                item.getSimpleName(),
                args.size() > 0 ? Optional.of(args) : Optional.empty(),
                resolveSpoonType(item.getType()));
    }

    @Override
    public Map<UFI, CtExecutable> mappings() {
        return null;
    }
}
