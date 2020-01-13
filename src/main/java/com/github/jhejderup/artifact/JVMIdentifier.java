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
package com.github.jhejderup.artifact;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spoon.reflect.declaration.CtExecutable;
import spoon.reflect.declaration.CtParameter;
import spoon.reflect.declaration.CtType;
import spoon.reflect.reference.CtArrayTypeReference;
import spoon.reflect.reference.CtTypeReference;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public final class JVMIdentifier {

  private static Logger              logger     = LoggerFactory
      .getLogger(JVMIdentifier.class);
  private static Map<String, String> spoonToJVM = new HashMap<>();

  static {
    spoonToJVM.put("byte", "B");
    spoonToJVM.put("char", "C");
    spoonToJVM.put("double", "D");
    spoonToJVM.put("float", "F");
    spoonToJVM.put("int", "I");
    spoonToJVM.put("long", "J");
    spoonToJVM.put("short", "S");
    spoonToJVM.put("boolean", "Z");
    spoonToJVM.put("void", "V");
  }

  public final String                 clazzName;
  public final String                 methodName;
  public final String                 methodDesc;
  public final Optional<CtExecutable> method;

  public JVMIdentifier(String clazzName, String methodName, String methodDesc,
      Optional<CtExecutable> method) {
    //TODO: Remove method, CtExecutable is specific to Spoon and instead create an interface

    this.clazzName = clazzName;
    this.methodName = methodName.replace("<", "&lt;").replace(">", "&gt;");
    this.methodDesc = methodDesc;
    this.method = method;
  }

  public static JVMIdentifier SpoonToJVMString(CtExecutable item) {
    var clazz = ((CtType) (item.getParent())).getReference();
    var ret = item.getType();
    var args = Arrays.stream(item.getParameters().toArray())
        .map(CtParameter.class::cast).map(arg -> toJVMType(arg.getType(), true))
        .collect(Collectors.joining(""));

    return new JVMIdentifier(toJVMType(clazz, false), item.getSimpleName(),
        "(" + args + ")" + toJVMType(ret, true), Optional.of(item));

  }

  private static String toJVMType(CtTypeReference type, boolean isMethodDesc) {
    var JVMType = new StringBuilder();

    if (type instanceof CtArrayTypeReference) {
      var brackets = ((CtArrayTypeReference) type).getDimensionCount();
      IntStream.rangeClosed(1, brackets).forEach(i -> JVMType.append("["));
    }

    if (type.isPrimitive()) {
      JVMType.append(spoonToJVM.get(type.getSimpleName()));
    } else {
      JVMType.append("L");
      JVMType.append(type.getQualifiedName().replace(".", "/"));
      if (isMethodDesc)
        JVMType.append(";");
    }
    return JVMType.toString();
  }

  public String getSignature() {
    return String.format("%s%s", this.methodName, this.methodDesc);
  }

  @Override
  public String toString() {
    return String
        .format("%s/%s%s", this.clazzName, this.methodName, this.methodDesc);
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
    JVMIdentifier id = (JVMIdentifier) obj;
    return Objects.equals(this.toString(), id.toString());
  }
}
