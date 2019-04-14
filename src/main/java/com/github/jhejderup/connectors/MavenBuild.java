package com.github.jhejderup.connectors;

import com.github.jhejderup.data.type.MavenCoordinate;
import com.github.jhejderup.data.type.MavenResolvedCoordinate;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.jboss.shrinkwrap.resolver.api.maven.MavenArtifactInfo;
import org.jboss.shrinkwrap.resolver.api.maven.MavenResolvedArtifact;
import org.jboss.shrinkwrap.resolver.api.maven.embedded.BuiltProject;
import org.jboss.shrinkwrap.resolver.api.maven.embedded.EmbeddedMaven;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MavenBuild {

    private static Stream<MavenArtifactInfo> getDependencies(MavenArtifactInfo artifact) {
        return Stream.concat(
                Stream.of(artifact),
                Arrays.stream(artifact.getDependencies()).flatMap(MavenBuild::getDependencies));
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
                .flatMap(MavenBuild::getDependencies)
                .map(MavenBuild::resolveMavenArtifactInfo);

    }

    public static List<MavenResolvedCoordinate> makeClasspath(Path project) {
        BuiltProject buildProject = EmbeddedMaven
                .forProject(project.toFile())
                .useDefaultDistribution()
                .setGoals("package")
                .build();

        String artifactName = buildProject.getDefaultBuiltArchive().getName();

        MavenResolvedArtifact[] artifacts = org.jboss.shrinkwrap.resolver.api.maven.Maven.resolver()
                .loadPomFromFile(buildProject.getModel().getPomFile())
                .importCompileAndRuntimeDependencies()
                .resolve().withTransitivity().asResolvedArtifact();

        MavenResolvedCoordinate client = new MavenResolvedCoordinate(
                buildProject.getModel().getGroupId(),
                buildProject.getModel().getName(),
                buildProject.getModel().getVersion(),
                Paths.get(buildProject.getTargetDirectory().toString(), artifactName));


        return Stream.concat(
                Stream.of(client),
                Arrays.stream(artifacts).map(MavenResolvedCoordinate::of))
                .collect(Collectors.toList());
    }
}
