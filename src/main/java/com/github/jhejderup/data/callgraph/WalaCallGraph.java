package com.github.jhejderup.data.callgraph;

import com.github.jhejderup.data.type.MavenCoordinate;
import com.github.jhejderup.data.type.MavenResolvedCoordinate;
import com.ibm.wala.ipa.callgraph.CallGraph;

import java.io.Serializable;
import java.util.List;

public final class WalaCallGraph implements Serializable {

    public final CallGraph walaGraph;
    public final List<MavenResolvedCoordinate> classPath;

    public WalaCallGraph(CallGraph walaGraph, List<MavenResolvedCoordinate> classPath) {
        this.walaGraph = walaGraph;
        this.classPath = classPath;
    }
}
