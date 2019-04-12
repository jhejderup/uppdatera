package com.github.jhejderup.data.type;

import org.gradle.tooling.model.GradleModuleVersion;
import org.gradle.tooling.model.idea.IdeaSingleEntryLibraryDependency;
import org.jboss.shrinkwrap.resolver.api.maven.MavenResolvedArtifact;

import java.io.Serializable;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.Optional;


public class MavenCoordinate implements Serializable, Namespace {

    public final String artifactId;
    public final String groupId;
    public final String version;


    public MavenCoordinate(String groupId, String artifactId, String version) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;

    }

    public String getCanonicalForm() {
        return String.join(":",
                this.groupId,
                this.artifactId,
                this.version);
    }

    public static MavenCoordinate of(String canonicalform) {
        String[] segments = canonicalform.split(":");
        assert segments.length == 3;
        return new MavenCoordinate(segments[0], segments[1], segments[2]);

    }

    @Override
    public String toString() {
        return "MavenCoordinate(" + this.artifactId + ","
                + this.artifactId + ","
                + this.version + ")";
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
        return new String[]{this.groupId, this.artifactId, this.version};
    }
}
