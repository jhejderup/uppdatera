package com.github.jhejderup.callgraph;

import java.io.Serializable;

public class ResolvedCall implements Serializable {

    private final ResolvedMethod source;
    private final ResolvedMethod target;

    public ResolvedCall(ResolvedMethod source, ResolvedMethod target) {
        this.source = source;
        this.target = target;
    }

    public ResolvedMethod getSource() {
        return source;
    }

    public ResolvedMethod getTarget() {
        return target;
    }
}
