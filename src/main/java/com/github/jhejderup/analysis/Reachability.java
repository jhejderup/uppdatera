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

import com.github.jhejderup.artifact.JVMIdentifier;
import com.github.jhejderup.callgraph.ResolvedCall;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.MethodReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class Reachability {

  private static Logger                                      logger = LoggerFactory
      .getLogger(Reachability.class);
  private final  Map<MethodReference, List<MethodReference>> graph;
  private final  Map<JVMIdentifier, MethodReference>         lookup;

  public Reachability(List<ResolvedCall> cg) {
    this.graph = new HashMap<>();
    this.lookup = new HashMap<>();

    cg.stream().filter(call -> !getClassLoader(call.target)
        .equals(ClassLoaderReference.Primordial)).forEach(call -> {
      //populate lookup table
      var target = WALAToJVMIdentifier(call.target);
      var source = WALAToJVMIdentifier(call.source);

      if (!this.lookup.containsKey(target)) {
        this.lookup.put(target, call.target);
      }
      if (!lookup.containsKey(source)) {
        this.lookup.put(target, call.source);
      }
      //populate a "reverse graph"
      if (this.graph.containsKey(call.target)) {
        this.graph.get(call.target).add(call.source);
      } else {
        this.graph.put(call.target,
            Stream.of(call.source).collect(Collectors.toList()));
      }
    });
  }

  private static JVMIdentifier WALAToJVMIdentifier(MethodReference ref) {
    return new JVMIdentifier(ref.getDeclaringClass().getName().toString(),
        ref.getName().toString(), ref.getDescriptor().toString());
  }

  public static ClassLoaderReference getClassLoader(MethodReference m) {
    return m.getDeclaringClass().getClassLoader();
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

    if (!getClassLoader(root).equals(ClassLoaderReference.Extension)) {
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
      if (getClassLoader(vertex).equals(ClassLoaderReference.Application)) {
        path.add(vertexName);
        return path;
      } else if (!visited.contains(vertexName)) {
        //is it the last node? (e.g., has no adjacent nodes), then none to queue
        if (this.graph.containsKey(vertex)) {
          var neighbours = this.graph.get(vertex);
          for (var neighbour : neighbours) {
            var new_path = new ArrayList<>(path);
            new_path.add(WALAToJVMIdentifier(neighbour));
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
