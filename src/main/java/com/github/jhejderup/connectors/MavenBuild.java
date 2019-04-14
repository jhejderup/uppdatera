package com.github.jhejderup.connectors;

import com.github.jhejderup.data.ModuleClasspath;
import com.github.jhejderup.data.type.MavenCoordinate;
import com.github.jhejderup.data.type.MavenResolvedCoordinate;
import com.ibm.wala.properties.WalaProperties;
import com.ibm.wala.util.WalaException;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.jboss.shrinkwrap.resolver.api.maven.MavenArtifactInfo;
import org.jboss.shrinkwrap.resolver.api.maven.MavenResolvedArtifact;
import org.jboss.shrinkwrap.resolver.api.maven.embedded.BuiltProject;
import org.jboss.shrinkwrap.resolver.api.maven.embedded.EmbeddedMaven;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MavenBuild {

    private static File WALA_COMPATIBLE_JDK;

    static {
        try {
            WALA_COMPATIBLE_JDK = new File(WalaProperties.loadProperties().getProperty(WalaProperties.J2SE_DIR));
        } catch (WalaException e) {
            e.printStackTrace();
        }
    }

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

    private static ModuleClasspath of(BuiltProject module){

        var artifactName = module.getDefaultBuiltArchive().getName();


        var project = new MavenResolvedCoordinate(
                module.getModel().getGroupId(),
                module.getModel().getName(),
                module.getModel().getVersion(),
                Paths.get(module.getTargetDirectory().toString(), artifactName));

        try {
            var dependencies = Maven.resolver()
                    .loadPomFromFile(module.getModel().getPomFile())
                    .importCompileAndRuntimeDependencies()
                    .resolve().withTransitivity().asResolvedArtifact();


            var resolvedDependencies = Arrays.stream(dependencies)
                    .map(MavenResolvedCoordinate::of)
                    .collect(Collectors.toList());

            return new ModuleClasspath(project, Optional.of(resolvedDependencies));

        } catch (Exception e) {

            return new ModuleClasspath(project, Optional.empty());

        }


    }

    public static List<ModuleClasspath> resolveClasspath(Path project) {


        var buildProject = EmbeddedMaven
                .forProject(project.toFile())
                .setJavaHome(WALA_COMPATIBLE_JDK)
                .useDefaultDistribution()
                .setGoals("package")
                .build();

        return buildProject
                .getModules()
                .stream()
                .map(MavenBuild::of)
                .collect(Collectors.toList());
    }
}
