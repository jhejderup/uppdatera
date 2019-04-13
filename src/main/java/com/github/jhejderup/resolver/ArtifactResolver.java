package com.github.jhejderup.resolver;


import com.github.jhejderup.data.type.MavenCoordinate;
import com.github.jhejderup.data.type.MavenResolvedCoordinate;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.jboss.shrinkwrap.resolver.api.maven.MavenArtifactInfo;
import org.jboss.shrinkwrap.resolver.api.maven.MavenResolvedArtifact;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ArtifactResolver {

    private static Stream<MavenArtifactInfo> getDependencies(MavenArtifactInfo artifact) {
        return Stream.concat(
                Stream.of(artifact),
                Arrays.stream(artifact.getDependencies()).flatMap(ArtifactResolver::getDependencies));
    }

    private static MavenResolvedArtifact resolveMavenArtifactInfo(MavenArtifactInfo info) {
        if (info instanceof MavenResolvedArtifact) {
            return (MavenResolvedArtifact) info;
        } else {
            return Maven.resolver().resolve(info.getCoordinate().toCanonicalForm())
                    .withoutTransitivity().asSingleResolvedArtifact();
        }
    }

    public static Stream<MavenResolvedArtifact> resolveDependencyTree(MavenCoordinate coordinate) {

        MavenResolvedArtifact[] artifacts = Maven.resolver()
                .resolve(coordinate.getCanonicalForm())
                .withTransitivity()
                .asResolvedArtifact();

        return Arrays.stream(artifacts)
                .flatMap(ArtifactResolver::getDependencies)
                .map(ArtifactResolver::resolveMavenArtifactInfo);

    }

    public static Stream<Map<String, MavenResolvedCoordinate>> getClasses(MavenResolvedArtifact artifact) {
        try {
            JarFile jar = new JarFile(artifact.asFile());
            return Stream.of(jar.stream()
                    .filter(entry -> entry.getName().endsWith(".class"))
                    .map(entry -> entry.getName().replaceAll("/", "\\."))
                    .map(ArtifactResolver::removeExtension)
                    .collect(Collectors.toMap(Function.identity(), k -> MavenResolvedCoordinate.of(artifact))));

        } catch (IOException e) {
            e.printStackTrace();
            return Stream.empty();

        }
    }

    private static String removeExtension(String fileName) {
        return fileName.substring(0, fileName.lastIndexOf("."));
    }

}
