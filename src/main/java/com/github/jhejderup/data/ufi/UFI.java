package com.github.jhejderup.data.ufi;

import com.github.jhejderup.data.ufi.UniversalType;

import java.io.Serializable;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public final class UFI implements Serializable {
    public final UniversalType pathType;
    public final String methodName;
    public final Optional<List<UniversalType>> parameters;
    public final UniversalType returnType;

    public final static String DELIM = "::";

    public UFI(UniversalType pathType,
               String methodName,
               Optional<List<UniversalType>> parameters,
               UniversalType returnType) {
        this.pathType = pathType;
        this.methodName = methodName;
        this.parameters = parameters;
        this.returnType = returnType;
    }

    @Override
    public String toString() {

        String args = parameters.isPresent() ?
                parameters.get()
                        .stream()
                        .map(t -> String.join(DELIM, t.getSegments()))
                        .collect( Collectors.joining(",") ) : "";


        return String.join(DELIM, this.pathType.getSegments()) + DELIM
                + this.methodName
                + "(" + args+ ")"
                + String.join(DELIM, this.returnType.getSegments());

    }


}
