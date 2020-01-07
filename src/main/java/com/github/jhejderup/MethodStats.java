package com.github.jhejderup;

import spoon.reflect.declaration.CtExecutable;

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
public final class MethodStats {

  public final int srcNumOfStmts;
  public final int dstNumOfStmts;

  public final CtExecutable srcMethod;
  public final CtExecutable dstMethod;

  public MethodStats(int srcNumOfStmts, int dstNumOfStmts,
      CtExecutable srcMethod, CtExecutable dstMethod) {
    this.srcNumOfStmts = srcNumOfStmts;
    this.dstNumOfStmts = dstNumOfStmts;
    this.srcMethod = srcMethod;
    this.dstMethod = dstMethod;
  }

  @Override
  public String toString() {
    return this.srcMethod.getSignature() + " -> " + this.dstMethod
        .getSignature() + ": " + this.srcNumOfStmts + " -> "
        + this.dstNumOfStmts;
  }
}
