package com.github.jhejderup.artifact;

import java.util.Objects;

public final class JVMIdentifier {

  public final String clazzName;
  public final String methodName;
  public final String methodDesc;

  public JVMIdentifier(String clazzName, String methodName, String methodDesc) {
    this.clazzName = clazzName;
    this.methodName = methodName;
    this.methodDesc = methodDesc;
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
