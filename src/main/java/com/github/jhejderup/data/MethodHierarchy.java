package com.github.jhejderup.data;

import com.ibm.wala.classLoader.IMethod;

import java.io.Serializable;

public class MethodHierarchy implements Serializable {

    public final IMethod child;
    public final IMethod parent;
    public final Relation type;

    public MethodHierarchy(IMethod child, Relation type, IMethod parent) {
        this.child = child;
        this.parent = parent;
        this.type = type;
    }

    public enum Relation {
        IMPLEMENTS,
        OVERRIDES,
    }

}
