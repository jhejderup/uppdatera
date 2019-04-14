package com.github.jhejderup.data.diff;

import com.github.jhejderup.data.type.*;
import com.github.jhejderup.data.ufi.UFI;
import com.github.jhejderup.data.ufi.UniversalArrayType;
import com.github.jhejderup.data.ufi.UniversalFunctionIdentifier;
import com.github.jhejderup.data.ufi.UniversalType;
import spoon.reflect.declaration.CtExecutable;
import spoon.reflect.declaration.CtParameter;
import spoon.reflect.declaration.CtType;
import spoon.reflect.reference.CtArrayTypeReference;
import spoon.reflect.reference.CtTypeReference;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toMap;

public final class ArtifactDiff implements Serializable, UniversalFunctionIdentifier<CtExecutable> {

    public final MavenCoordinate left;
    public final MavenCoordinate right;
    public final List<JavaSourceDiff> diff;
    public final List<MavenResolvedCoordinate> analyzedClasspath;
    public final Map<String, MavenResolvedCoordinate> classToCoordinate;

    public ArtifactDiff(MavenCoordinate left,
                        MavenCoordinate right,
                        List<JavaSourceDiff> diff,
                        List<MavenResolvedCoordinate> analyzedClasspath) {
        this.left = left;
        this.right = right;
        this.diff = diff;
        this.analyzedClasspath = analyzedClasspath;
        this.classToCoordinate = createLookupTable();
    }


    private Map<String, MavenResolvedCoordinate> createLookupTable() {
        return this.analyzedClasspath
                .stream()
                .map(MavenResolvedCoordinate::getClasses)
                .flatMap(m -> m.entrySet().stream())
                .distinct()
                .collect(toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private UniversalType resolveSpoonType(CtTypeReference type) {

        if (type.isPrimitive()) {

            if (type instanceof CtArrayTypeReference) {
                return new UniversalArrayType(
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
                    return new UniversalArrayType(
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
                        return new UniversalArrayType(
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

        return this.diff.stream()
                .filter(s -> s.methodDiffs.isPresent())
                .flatMap(s -> s.methodDiffs.get().keySet().stream())
                .collect(toMap(s -> convertToUFI(s), Function.identity()));
    }
}
