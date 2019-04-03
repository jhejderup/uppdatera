package com.github.jhejderup.data.ufi;

import com.github.jhejderup.data.JDKClassPath;
import com.github.jhejderup.data.MavenCoordinate;
import com.github.jhejderup.data.Namespace;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

public final class UniversalType implements Serializable {
    public final Optional<Namespace> outer;
    public final Namespace inner;

    public UniversalType(Optional<Namespace> outer, Namespace inner) {
        this.outer = outer;
        this.inner = inner;
    }

    public String[] getSegments(){

        if(outer.isPresent()) {

            Namespace global = outer.get();

            String repoPrefix = "std";
            if (global instanceof MavenCoordinate){
                repoPrefix = "mvn";

            } else if (global instanceof JDKClassPath){
                repoPrefix = "jdk";
            }

            return Stream.concat(
                    Stream.concat(Stream.of(repoPrefix), Arrays.stream(global.segments)),
                    Arrays.stream(inner.segments)).toArray(String[]::new);

        } else {
            return Stream.concat(Stream.of("__unknown"),Arrays.stream(inner.segments)).toArray(String[]::new);
        }

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
        UniversalType ty = (UniversalType) o;
        return Objects.equals(this.outer,ty.outer) &&
                Objects.equals(this.inner, ty.inner);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.outer,this.inner);
    }
}
