package com.github.jhejderup.data;

import java.io.Serializable;

public class Namespace implements Serializable {

    public final String[] segments;

    public Namespace(String... segments){
        this.segments = segments;
    }

}
