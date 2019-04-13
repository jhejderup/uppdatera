package com.github.jhejderup.data.ufi;

import com.github.jhejderup.data.type.Namespace;

import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public final class ArrayType extends UniversalType implements Namespace {

    public final int brackets;

    public ArrayType(Optional<Namespace> outer, Namespace inner, int brackets) {
        super(outer, inner);
        this.brackets = brackets;
    }


    @Override
    public String[] getSegments() {
        String brackets = IntStream
                .rangeClosed(1, this.brackets)
                .mapToObj(i -> "[]").collect(Collectors.joining(""));

        String[] segments = super.getSegments().clone();

        String lastElement = segments[segments.length - 1];

        segments[segments.length - 1] = lastElement + brackets;

        return segments;
    }
}
