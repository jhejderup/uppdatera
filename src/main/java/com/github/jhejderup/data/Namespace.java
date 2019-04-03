package com.github.jhejderup.data;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Objects;

public class Namespace implements Serializable {

    public final String[] segments;

    public Namespace(String... segments){
        this.segments = segments;
    }

    @Override
    public boolean equals(Object o) {
        // self check
        if (this == o)
            return true;
        // null check
        if (o == null)
            return false;
        // type check and cast
        if (getClass() != o.getClass())
            return false;
        Namespace ns = (Namespace) o;
        return Arrays.equals(this.segments, ns.segments);
    }

    @Override
    public int hashCode() {
        return Objects.hash(segments);
    }
}
