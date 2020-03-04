package com.github.jhejderup.callgraph;

import java.io.Serializable;
import java.util.Objects;

public final class ResolvedCall implements Serializable {

    private final ResolvedMethod source;
    private final ResolvedMethod target;

    public ResolvedCall(ResolvedMethod source, ResolvedMethod target) {
        this.source = source;
        this.target = target;
    }

    public ResolvedMethod getSource() {
        return source;
    }

    public ResolvedMethod getTarget() {
        return target;
    }

    @Override
    public String toString() {
        return getSource().toString() + " -> " + getTarget().toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ResolvedCall that = (ResolvedCall) o;
        return Objects.equals(source, that.source) &&
                Objects.equals(target, that.target);
    }

    @Override
    public int hashCode() {
        return Objects.hash(source, target);
    }
}
