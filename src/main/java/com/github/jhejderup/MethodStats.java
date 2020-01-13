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
package com.github.jhejderup;

import com.github.jhejderup.artifact.JVMIdentifier;

import java.util.Objects;
import java.util.Optional;

public final class MethodStats {

  public final Optional<JVMIdentifier> srcMethod;
  public final Optional<JVMIdentifier> dstMethod;

  public MethodStats(
      Optional<JVMIdentifier> srcMethod, Optional<JVMIdentifier> dstMethod) {

    this.srcMethod = srcMethod;
    this.dstMethod = dstMethod;

  }

  public JVMIdentifier getSrcMethod()  {
    return this.srcMethod.get();
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(this.toString());
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    MethodStats id = (MethodStats) obj;
    return Objects.equals(this.srcMethod, id.srcMethod) && Objects
        .equals(this.dstMethod, id.dstMethod);
  }

  @Override
  public String toString() {
    if (dstMethod.isPresent()) {
      return this.srcMethod.get().getSignature() + " -> " + this.dstMethod.get()
          .getSignature();

    } else {
      return this.srcMethod.get().getSignature() + " -> None";
    }
  }
}
