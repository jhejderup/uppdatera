package com.github.jhejderup.artifact;

import spoon.reflect.declaration.CtExecutable;
import spoon.reflect.declaration.CtParameter;
import spoon.reflect.declaration.CtType;
import spoon.reflect.reference.CtArrayTypeReference;
import spoon.reflect.reference.CtTypeReference;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public final class JVMIdentifier {

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

  public final String clazzName;
  public final String methodName;
  public final String methodDesc;
  public JVMIdentifier(String clazzName, String methodName, String methodDesc) {
    this.clazzName = clazzName;
    this.methodName = methodName;
    this.methodDesc = methodDesc;
  }

  public static JVMIdentifier SpoonToJVMString(CtExecutable item) {
    var clazz = ((CtType) (item.getParent())).getReference();
    var ret = item.getType();
    var args = Arrays.stream(item.getParameters().toArray())
        .map(CtParameter.class::cast).map(arg -> toJVMType(arg.getType(), true))
        .collect(Collectors.joining(""));

    return new JVMIdentifier(toJVMType(clazz, false), item.getSimpleName(),
        "(" + args + ")" + toJVMType(ret, true));

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
    return String
        .format("%s%s", this.methodName, this.methodDesc);
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
