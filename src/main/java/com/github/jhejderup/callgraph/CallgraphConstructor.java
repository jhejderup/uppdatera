package com.github.jhejderup.callgraph;

import java.util.List;

public interface CallgraphConstructor {

    List<ResolvedCall> build(String projectClassPath, String dependencyClassPath);


}
