package com.github.jhejderup.connectors;


import com.github.jhejderup.data.ModuleClasspath;
import com.github.jhejderup.data.type.MavenResolvedCoordinate;
import org.gradle.tooling.GradleConnector;
import org.gradle.tooling.ProjectConnection;
import org.gradle.tooling.model.idea.IdeaModule;
import org.gradle.tooling.model.idea.IdeaProject;
import org.gradle.tooling.model.idea.IdeaSingleEntryLibraryDependency;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static java.util.stream.Collectors.toList;

public class GradleBuild {


    private static ProjectConnection connect(Path gradleProject) {
        return GradleConnector.newConnector()
                .forProjectDirectory(gradleProject.toFile())
                .connect();
    }


    private static void compile(Path gradleProject) {

        ProjectConnection connection = connect(gradleProject);

        try {
            // Configure the build
            var launcher = connection.newBuild();
            launcher.forTasks("jar");
            launcher.setStandardOutput(System.out);
            launcher.setStandardError(System.err);
            // Run the build
            launcher.run();
        } finally {
            // Clean up
            connection.close();

        }

    }

    private static File[] findThatJAR(String dirName) {
        var dir = new File(dirName);
        return dir.listFiles((dir1, filename) -> filename.endsWith(".jar"));
    }

    private static Optional<ModuleClasspath> getBuildProject(IdeaModule module) {

        String outputFolder = module.getGradleProject().getBuildDirectory().getAbsolutePath()
                + File.separator
                + "libs";

        var jarfiles = findThatJAR(outputFolder);

        var file = Arrays.stream(jarfiles)
                .filter(jarFile -> jarFile.toString().contains(module.getName()))
                .findFirst();

        if (file.isPresent()) {
            var resolvedDependencies = module.getDependencies()
                    .stream()
                    .map(k -> {
                        try {
                            return (IdeaSingleEntryLibraryDependency) k;
                        } catch (Exception e) {
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .filter(d -> !d.getScope().toString().contains("TEST"))
                    .map(MavenResolvedCoordinate::of)
                    .filter(Objects::nonNull)
                    .collect(toList());

            var project = new MavenResolvedCoordinate("com.github", module.getName(), "SNAPSHOT",
                    Paths.get(file.get().toURI()));

            return Optional.of(new ModuleClasspath(project, Optional.of(resolvedDependencies)));

        } else {
            return Optional.empty();
        }

    }


    public static List<ModuleClasspath> resolveClasspath(Path project) {
        ProjectConnection connection = connect(project);

        try {
            compile(project);
        } catch (Exception e) {
            throw e;
        }

        try {


            IdeaProject ideaProject = connection.getModel(IdeaProject.class);

            if (!ideaProject.getJavaLanguageSettings().getLanguageLevel().isJava8Compatible())
                try {
                    throw new Exception("Not Compatible with WALA, up to Java 8, sorry mate!");
                } catch (Exception e) {
                    e.printStackTrace();
                }

            return ideaProject
                    .getModules()
                    .stream()
                    .map(GradleBuild::getBuildProject)
                    .flatMap(Optional::stream)
                    .collect(toList());
        } finally {
            connection.close();
        }

    }
}