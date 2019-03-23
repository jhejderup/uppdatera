package com.github.jhejderup.data;

import java.io.Serializable;

public class MavenCoordinate implements Serializable {

    public final String artifactId;
    public final String groupId;
    public final String version;

    public MavenCoordinate(String groupId, String artifactId, String version) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
    }


}
