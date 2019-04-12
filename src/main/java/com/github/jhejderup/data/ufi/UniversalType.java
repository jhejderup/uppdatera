package com.github.jhejderup.data.ufi;

import com.github.jhejderup.data.type.JDKPackage;
import com.github.jhejderup.data.type.MavenCoordinate;
import com.github.jhejderup.data.type.Namespace;

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

            Namespace global = this.outer.get();

            String repoPrefix = "";
            if (global instanceof MavenCoordinate){
                repoPrefix = "mvn";

            } else if (global instanceof JDKPackage){
                repoPrefix = "jdk";
            }

            return Stream.concat(
                    Stream.concat(Stream.of(repoPrefix), Arrays.stream(global.getSegments())),
                    Arrays.stream(inner.getSegments())).toArray(String[]::new);

        } else {
            return Arrays.stream(inner.getSegments()).toArray(String[]::new);
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
