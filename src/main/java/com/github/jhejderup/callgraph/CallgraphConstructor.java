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
package com.github.jhejderup.callgraph;

import java.util.List;

public interface CallgraphConstructor {

    /**
     * Builds the callgraph of a project and it's dependencies, with all public non-abstract non-private methods
     * in the project as entry points. The resulting callgraph is used to create a list of {@link ResolvedCall},
     * which is then returned.
     *
     * @param projectClassPath path to the classpath of the project
     * @param dependencyClassPath paths to the dependency jars, separated by colons (':')
     * @return the list of resolved calls
     * @throws CallgraphException whenever an unexpected error occurs during the building phase of the callgraph (currently only applicable to the Wala implementation)
     */
    List<ResolvedCall> build(String projectClassPath, String dependencyClassPath) throws CallgraphException;


}
