package com.github.jhejderup.data.callgraph;

import com.ibm.wala.types.MethodReference;

import java.io.Serializable;

public final class ResolvedCall implements Serializable {

    public final MethodReference target;
    public final MethodReference source;


    public ResolvedCall(MethodReference source, MethodReference target) {
        this.source = source;
        this.target = target;
    }
}
