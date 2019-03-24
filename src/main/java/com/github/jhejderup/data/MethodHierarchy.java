package com.github.jhejderup.data;

import com.ibm.wala.classLoader.IMethod;

import java.io.Serializable;
import java.util.Optional;

public class MethodHierarchy implements Serializable {

    public final IMethod child;
    public final Optional<IMethod> parent;
    public final Relation type;

    public MethodHierarchy(IMethod child, Relation type, Optional<IMethod>
            parent) {
        this.child = child;
        this.parent = parent;
        this.type = type;
    }

    public enum Relation {
        IMPLEMENTS,
        OVERRIDES,
        CONCRETE
    }

}
