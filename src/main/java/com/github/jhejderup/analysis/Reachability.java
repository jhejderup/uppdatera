/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.jhejderup.analysis;

import com.github.jhejderup.callgraph.JVMIdentifier;
import com.github.jhejderup.callgraph.MethodScope;
import com.github.jhejderup.callgraph.ResolvedCall;
import com.github.jhejderup.callgraph.ResolvedMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class Reachability {

    private static Logger logger = LoggerFactory
            .getLogger(Reachability.class);
    private final Map<ResolvedMethod, List<ResolvedMethod>> graph;
    private final Map<JVMIdentifier, ResolvedMethod> lookup;

    public Reachability(List<ResolvedCall> cg) {
        this.graph = new HashMap<>();
        this.lookup = new HashMap<>();

        cg.stream().filter(call -> call.getTarget().getContext() != MethodScope.PRIMORDIAL).forEach(call -> {
            //populate lookup table
            var target = call.getTarget().getIdentifier();
            var source = call.getSource().getIdentifier();

            if (!this.lookup.containsKey(target)) {
                this.lookup.put(target, call.getTarget());
            }
            if (!lookup.containsKey(source)) {
                this.lookup.put(target, call.getSource());
            }
            //populate a "reverse graph"
            if (this.graph.containsKey(call.getTarget())) {
                this.graph.get(call.getTarget()).add(call.getSource());
            } else {
                this.graph.put(call.getTarget(), Stream.of(call.getSource()).collect(Collectors.toList()));
            }
        });
    }

    public List<JVMIdentifier> search(JVMIdentifier methodID) {
        ///
        /// Validate node
        ///
        if (!this.lookup.containsKey(methodID)) {
            logger.info(
                    "[search] the function `" + methodID + "` is not called by the user");
            return new ArrayList<>();
        }

        var root = this.lookup.get(methodID);

        if (root.getContext() != MethodScope.DEPENDENCY) {
            logger.error("[search] the function `" + methodID
                    + "` is not a dependency node (e.g., Extension type)");
            return new ArrayList<>();
        }

        ///
        /// Search
        ///
        var queue = new LinkedList<List<JVMIdentifier>>();
        var visited = new HashSet<JVMIdentifier>();
        queue.add(Stream.of(methodID).collect(Collectors.toList()));

        while (queue.size() > 0) {
            // Get first path in the queue
            var path = queue.pop();

            // Get the last node of that path
            var vertexName = path.get(path.size() - 1);
            var vertex = this.lookup.get(vertexName);

            //is this an application node?
            if (vertex.getContext() == MethodScope.APPLICATION) {
                var appNode = vertex.getIdentifier();
                if (!appNode.equals(vertexName)) {
                    path.add(vertexName);
                }
                path.add(vertex.getIdentifier());
                return path;
            } else if (!visited.contains(vertexName)) {
                //is it the last node? (e.g., has no adjacent nodes), then none to queue
                if (this.graph.containsKey(vertex)) {
                    var neighbours = this.graph.get(vertex);
                    for (var neighbour : neighbours) {
                        var new_path = new ArrayList<>(path);
                        new_path.add(neighbour.getIdentifier());
                        //add to end of the list
                        queue.add(new_path);
                    }
                }
                // Mark node as visited!
                visited.add(vertexName);
            }
        }
        return new ArrayList<>();
    }
}
