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

import java.util.Objects;

/**
 * Represents a resolved method.
 * <p>
 * A resolved method is a method of which the JVMIdentifier and the class loader scope have been determined.
 */
public abstract class ResolvedMethod {

    /**
     * Returns the JVMIdentifier of this resolved method.
     *
     * @return the JVMIdentifier
     */
    abstract public JVMIdentifier getIdentifier();

    /**
     * Returns the class loader scope of this method.
     *
     * @return the class loader scope
     */
    abstract public MethodScope getScope();

    @Override
    public String toString() {
        return getIdentifier().toString() + " [" + getScope().name() + "]";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || !(o instanceof ResolvedMethod)) return false;
        ResolvedMethod that = (ResolvedMethod) o;
        return getIdentifier().equals(that.getIdentifier()) &&
                getScope().equals(that.getScope());
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.toString());
    }

}
