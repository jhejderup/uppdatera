package com.github.jhejderup.data.callgraph;

import com.github.jhejderup.data.type.MavenResolvedCoordinate;
import com.ibm.wala.ipa.callgraph.CallGraph;

import java.io.Serializable;
import java.util.List;

public final class WalaCallGraph implements Serializable {

    public final CallGraph rawcg;
    public final List<MavenResolvedCoordinate> analyzedClasspath;

    public WalaCallGraph(CallGraph rawcg, List<MavenResolvedCoordinate> analyzedClasspath) {
        this.rawcg = rawcg;
        this.analyzedClasspath = analyzedClasspath;
    }
}