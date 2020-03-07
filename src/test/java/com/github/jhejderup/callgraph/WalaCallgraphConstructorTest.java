package com.github.jhejderup.callgraph;

import com.github.jhejderup.callgraph.wala.WalaCallgraphConstructor;

public class WalaCallgraphConstructorTest extends CallgraphConstructorTest {

    @Override
    protected CallgraphConstructor getConstructor() {
        return new WalaCallgraphConstructor();
    }

}
