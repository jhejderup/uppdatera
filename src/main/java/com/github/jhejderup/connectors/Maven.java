package com.github.jhejderup.connectors;

import com.github.jhejderup.data.type.MavenResolvedCoordinate;
import org.jboss.shrinkwrap.resolver.api.maven.MavenResolvedArtifact;
import org.jboss.shrinkwrap.resolver.api.maven.embedded.BuiltProject;
import org.jboss.shrinkwrap.resolver.api.maven.embedded.EmbeddedMaven;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Maven {

    public static List<MavenResolvedCoordinate> getClasspath(Path mavenProject) {
        BuiltProject project = EmbeddedMaven
                .forProject(mavenProject.toFile())
                .useDefaultDistribution()
                .setGoals("package")
                .build();

        String artifactName = project.getDefaultBuiltArchive().getName();

        MavenResolvedArtifact[] artifacts = org.jboss.shrinkwrap.resolver.api.maven.Maven.resolver()
                .loadPomFromFile(project.getModel().getPomFile())
                .importCompileAndRuntimeDependencies()
                .resolve().withTransitivity().asResolvedArtifact();

        MavenResolvedCoordinate client = new MavenResolvedCoordinate(
                project.getModel().getGroupId(),
                project.getModel().getName(),
                project.getModel().getVersion(),
                Paths.get(project.getTargetDirectory().toString(), artifactName));


        return Stream.concat(
                Stream.of(client),
                Arrays.stream(artifacts).map(MavenResolvedCoordinate::of))
                .collect(Collectors.toList());

    }
}
