package com.github.jhejderup;


import com.github.jhejderup.data.MavenCoordinate;
import com.github.jhejderup.diff.ArtifactDiff;
import com.github.jhejderup.diff.ArtifactResolver;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;
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


    public static void main(String[] args) throws InterruptedException, TimeoutException, IOException {
        MavenCoordinate coord = new MavenCoordinate("com.squareup.okhttp3", "okcurl", "3.13.1");
        Map<String,MavenCoordinate> lookup = buildClassLookupTable(coord);

        lookup.entrySet().stream().forEach(System.out::println);



        ArtifactDiff
                .diff(new MavenCoordinate("com.squareup.okhttp3", "okhttp", "3.13.1"),
                        new MavenCoordinate("com.squareup.okhttp3", "okhttp", "3.14.0"))
                .forEach(diff -> System.out.println(diff.fileDiff.toString()));
    }


}
