package com.github.jhejderup.data;

import java.io.Serializable;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class MavenCoordinate extends Namespace implements Serializable {

    private final static Pattern pattern = Pattern.compile("m2\\/repository\\/?(?<group>.*)\\/(?<artifact>[^\\/]*)\\/(?<version>[^\\/]*)\\/([^\\/]*).jar");

    public final String artifactId;
    public final String groupId;
    public final String version;



    public MavenCoordinate(String groupId, String artifactId, String version) {
        super(new String[]{groupId.replace(".","::"),artifactId,version}); //TODO: we cheating here
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;

    }

    @Override
    public String toString() {
        return "Coordinate("+ this.artifactId + ","
                + this.artifactId + ","
                + this.version+")";
    }

    public String getCanonicalForm() {
        return this.groupId
                + ":"
                + this.artifactId
                + ":"
                + this.version;

    }

    public static MavenCoordinate of(String jarPath) {
        Matcher matcher = pattern.matcher(jarPath);
        matcher.find();
        return new MavenCoordinate(
                matcher.group("group").replace('/', '.'),
                matcher.group("artifact"),
                matcher.group("version")
        );
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
        MavenCoordinate coord = (MavenCoordinate) o;
        return super.equals(o) &&
                Objects.equals(this.groupId, coord.groupId) &&
                Objects.equals(this.artifactId, coord.artifactId) &&
                Objects.equals(this.version, coord.version);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.segments,this.groupId,this.artifactId,this.version);
    }
}
