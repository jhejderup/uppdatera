package com.github.jhejderup.callgraph;

import com.github.jhejderup.callgraph.opal.OpalCallgraphConstructor;

public class OpalCallgraphConstructorTest extends CallgraphConstructorTest {

    @Override
    protected CallgraphConstructor getConstructor() {
        return new OpalCallgraphConstructor();
    }

}
