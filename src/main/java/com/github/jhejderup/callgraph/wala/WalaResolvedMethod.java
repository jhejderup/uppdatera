package com.github.jhejderup.callgraph.wala;

import com.github.jhejderup.callgraph.JVMIdentifier;
import com.github.jhejderup.callgraph.MethodContext;
import com.github.jhejderup.callgraph.ResolvedMethod;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.MethodReference;

import java.util.Objects;

public class WalaResolvedMethod implements ResolvedMethod {

    JVMIdentifier identifier;
    MethodContext context;

    public WalaResolvedMethod(MethodReference methodReference) {
        identifier = JVMIdentifier.fromWalaMethodReference(methodReference);

        ClassLoaderReference classLoader = getClassLoader(methodReference);

        if (classLoader.equals(ClassLoaderReference.Application)) {
            context = MethodContext.APPLICATION;
        } else if (classLoader.equals(ClassLoaderReference.Extension)) {
            context = MethodContext.DEPENDENCY;
        } else if (classLoader.equals(ClassLoaderReference.Primordial)) {
            context = MethodContext.PRIMORDIAL;
        } else {
            context = MethodContext.UNKNOWN;
        }
    }

    private static ClassLoaderReference getClassLoader(MethodReference m) {
        return m.getDeclaringClass().getClassLoader();
    }

    @Override
    public JVMIdentifier getIdentifier() {
        return identifier;
    }

    @Override
    public MethodContext getContext() {
        return context;
    }

    @Override
    public String toString() {
        return identifier.toString() + " [" + context.name() + "]";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WalaResolvedMethod that = (WalaResolvedMethod) o;
        return identifier.equals(that.identifier) &&
                context == that.context;
    }

    @Override
    public int hashCode() {
        return Objects.hash(identifier, context);
    }
}
