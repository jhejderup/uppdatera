package com.github.jhejderup.data;

import com.github.jhejderup.data.type.MavenCoordinate;

import java.io.Serializable;

public final class UpdatePair implements Serializable {
    public final MavenCoordinate left;
    public final MavenCoordinate right;


    public UpdatePair(MavenCoordinate left, MavenCoordinate right) {
        this.left = left;
        this.right = right;
    }
}
