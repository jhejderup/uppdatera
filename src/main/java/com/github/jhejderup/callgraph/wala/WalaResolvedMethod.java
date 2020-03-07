package com.github.jhejderup.callgraph.wala;

import com.github.jhejderup.callgraph.JVMIdentifier;
import com.github.jhejderup.callgraph.MethodScope;
import com.github.jhejderup.callgraph.ResolvedMethod;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.MethodReference;

public final class WalaResolvedMethod extends ResolvedMethod {

    JVMIdentifier identifier;
    MethodScope context;

    public WalaResolvedMethod(MethodReference methodReference) {
        identifier = JVMIdentifier.fromWalaMethodReference(methodReference);

        ClassLoaderReference classLoader = getClassLoader(methodReference);

        if (classLoader.equals(ClassLoaderReference.Application)) {
            context = MethodScope.APPLICATION;
        } else if (classLoader.equals(ClassLoaderReference.Extension)) {
            context = MethodScope.DEPENDENCY;
        } else if (classLoader.equals(ClassLoaderReference.Primordial)) {
            context = MethodScope.PRIMORDIAL;
        } else {
            context = MethodScope.UNKNOWN;
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
    public MethodScope getScope() {
        return context;
    }

}
