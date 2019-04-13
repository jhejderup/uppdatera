package com.github.jhejderup.data.ufi;

import com.github.jhejderup.data.type.Namespace;

import java.util.Optional;


public final class UniversalArrayType extends UniversalType  {

    public final int numOfBrackets;

    public UniversalArrayType(Optional<Namespace> outer, Namespace inner, int numOfBrackets) {
        super(outer, inner);
        this.numOfBrackets = numOfBrackets;
    }
}
