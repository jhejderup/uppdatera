package com.github.jhejderup.data.type;

import com.g00fy2.versioncompare.Version;

import java.io.Serializable;
import java.util.Objects;


public class MavenCoordinate implements Serializable, Comparable<MavenCoordinate>, Namespace {

    public final String artifactId;
    public final String groupId;
    public final Version version;


    public MavenCoordinate(String groupId, String artifactId, String version) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = new Version(version);

    }

    public static MavenCoordinate of(String canonicalform) {
        String[] segments = canonicalform.split(":");
        assert segments.length == 3;
        return new MavenCoordinate(segments[0], segments[1], segments[2]);

    }

    public String getCanonicalForm() {
        return String.join(this.getNamespaceDelim(),
                this.groupId,
                this.artifactId,
                this.version.getOriginalString());
    }

    @Override
    public String toString() {
        return "MavenCoordinate(" + this.groupId + ","
                + this.artifactId + ","
                + this.version.getOriginalString() + ")";
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
        return
                Objects.equals(this.groupId, coord.groupId) &&
                        Objects.equals(this.artifactId, coord.artifactId) &&
                        Objects.equals(this.version, coord.version);
    }


    @Override
    public int hashCode() {
        return Objects.hash(this.groupId, this.artifactId, this.version);
    }

    @Override
    public String[] getSegments() {
        return new String[]{this.groupId, this.artifactId, this.version.getOriginalString()};
    }

    @Override
    public String getNamespaceDelim() {
        return ":";
    }

    @Override
    public int compareTo(MavenCoordinate o) {
        final int BEFORE = -1;
        final int EQUAL = 0;
        final int AFTER = 1;

        if (this.version.isEqual(o.version)) return EQUAL;
        if (this.version.isHigherThan(o.version)) return AFTER;
        return BEFORE;
    }
}
