package com.github.jhejderup.callgraph.wala;

import com.github.jhejderup.callgraph.JVMIdentifier;
import com.github.jhejderup.callgraph.MethodContext;
import com.github.jhejderup.callgraph.ResolvedMethod;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.MethodReference;

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
}
