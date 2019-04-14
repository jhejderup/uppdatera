package com.github.jhejderup.data.type;

import org.gradle.tooling.model.GradleModuleVersion;
import org.gradle.tooling.model.idea.IdeaSingleEntryLibraryDependency;
import org.jboss.shrinkwrap.resolver.api.maven.MavenResolvedArtifact;

import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.function.Function;
import java.util.jar.JarFile;

import static java.util.stream.Collectors.toMap;

public final class MavenResolvedCoordinate extends MavenCoordinate implements Serializable {
    public final Path jarPath;

    public MavenResolvedCoordinate(String groupId, String artifactId, String version, Path jarPath) {
        super(groupId, artifactId, version);
        this.jarPath = jarPath;
    }


    public static MavenResolvedCoordinate of(MavenResolvedArtifact artifact) {
        return new MavenResolvedCoordinate(
                artifact.getCoordinate().getGroupId(),
                artifact.getCoordinate().getArtifactId(),
                artifact.getCoordinate().getVersion(),
                artifact.as(Path.class));
    }



    public static MavenResolvedCoordinate of(IdeaSingleEntryLibraryDependency d) {
        GradleModuleVersion mod = d.getGradleModuleVersion();
        return new MavenResolvedCoordinate(
                mod.getGroup(),
                mod.getName(),
                mod.getVersion(),
                Paths.get(d.getFile().toURI()));
    }

    public static Map<String, MavenResolvedCoordinate> getClasses(MavenResolvedCoordinate coord) {
        try {
            JarFile jar = new JarFile(coord.jarPath.toFile());
            return jar.stream()
                    .filter(entry -> entry.getName().endsWith(".class"))
                    .map(entry -> entry.getName().replaceAll("/", "\\."))
                    .map(MavenResolvedCoordinate::removeExtension)
                    .collect(toMap(Function.identity(), k -> coord));

        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private static String removeExtension(String fileName) {
        return fileName.substring(0, fileName.lastIndexOf("."));
    }

}
