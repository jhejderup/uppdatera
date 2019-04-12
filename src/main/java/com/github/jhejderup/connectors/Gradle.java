package com.github.jhejderup.connectors;


import com.github.jhejderup.data.type.MavenResolvedCoordinate;
import org.gradle.tooling.BuildLauncher;
import org.gradle.tooling.GradleConnector;
import org.gradle.tooling.ProjectConnection;
import org.gradle.tooling.model.DomainObjectSet;
import org.gradle.tooling.model.idea.IdeaDependency;
import org.gradle.tooling.model.idea.IdeaModule;
import org.gradle.tooling.model.idea.IdeaProject;
import org.gradle.tooling.model.idea.IdeaSingleEntryLibraryDependency;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Gradle {


    private static ProjectConnection connect(Path gradleProject) {
        return GradleConnector.newConnector()
                .forProjectDirectory(gradleProject.toFile())
                .connect();
    }


    private static void compile(Path gradleProject) {

        ProjectConnection connection = connect(gradleProject);

        try {
            // Configure the build
            BuildLauncher launcher = connection.newBuild();
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

    public static List<MavenResolvedCoordinate> getClasspath(Path gradleProject) {
        ProjectConnection connection = connect(gradleProject);

        try {
            compile(gradleProject);
        } catch (Exception e) {
            throw e;
        }

        try {
            IdeaProject project = connection.getModel(IdeaProject.class);

            Map<IdeaModule, DomainObjectSet<? extends IdeaDependency>> dependencies = project
                    .getModules()
                    .stream()
                    .collect(Collectors.toMap(Function.identity(), IdeaModule::getDependencies));

            Map<IdeaModule, List<IdeaSingleEntryLibraryDependency>> compileDependencies =
                    dependencies
                            .entrySet()
                            .stream()
                            .collect(Collectors.toMap(
                                    e -> e.getKey(),
                                    e -> e.getValue().stream()
                                            .map(IdeaSingleEntryLibraryDependency.class::cast)
                                            .filter(d -> !d.getScope().toString().contains("TEST"))
                                            .collect(Collectors.toList())));


            IdeaModule root = project.getModules().getAt(0);
            String outputFolder = root.getGradleProject().getBuildDirectory().getAbsolutePath()
                    + File.separator
                    + "libs";

            File[] jarfile = finder(outputFolder);
            assert jarfile.length == 1;

            MavenResolvedCoordinate client =
                    new MavenResolvedCoordinate(
                            "localhost",
                            project.getName(),
                            "XXX",
                            Paths.get(jarfile[0].toURI()));


            return Stream.concat(
                    Stream.of(client),
                    compileDependencies.get(root).stream().map(MavenResolvedCoordinate::of))
                    .collect(Collectors.toList());

        } finally {
            connection.close();
        }

    }

    public static File[] finder(String dirName) {
        File dir = new File(dirName);
        return dir.listFiles((dir1, filename) -> filename.endsWith(".jar"));

    }
}