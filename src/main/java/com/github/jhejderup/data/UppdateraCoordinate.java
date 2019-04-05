package com.github.jhejderup.data;

import org.gradle.tooling.model.GradleModuleVersion;
import org.gradle.tooling.model.idea.IdeaSingleEntryLibraryDependency;

import java.io.Serializable;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class UppdateraCoordinate implements Serializable {
    public final String artifactId;
    public final String groupId;
    public final String version;
    public final Path  jarFile;


    public UppdateraCoordinate(String artifactId, String groupId, String version, Path jarFile) {
        this.artifactId = artifactId;
        this.groupId = groupId;
        this.version = version;
        this.jarFile = jarFile;
    }

    public static UppdateraCoordinate of(IdeaSingleEntryLibraryDependency d){
        GradleModuleVersion v = d.getGradleModuleVersion();
        return new UppdateraCoordinate(v.getName(),v.getGroup(),v.getVersion(), Paths.get(d.getFile().toURI()));
    }

}
