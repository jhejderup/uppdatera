package com.github.jhejderup.callgraph;

public interface ResolvedMethod {


    JVMIdentifier getIdentifier();

    MethodScope getContext();

}
