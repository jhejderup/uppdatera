package com.github.jhejderup.connectors;


import com.github.jhejderup.data.UppdateraCoordinate;
import org.gradle.tooling.BuildLauncher;
import org.gradle.tooling.GradleConnector;
import org.gradle.tooling.ProjectConnection;
import org.gradle.tooling.model.DomainObjectSet;
import org.gradle.tooling.model.idea.IdeaDependency;
import org.gradle.tooling.model.idea.IdeaModule;
import org.gradle.tooling.model.idea.IdeaProject;
import org.gradle.tooling.model.idea.IdeaSingleEntryLibraryDependency;


import javax.naming.ConfigurationException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class EmbeddedGradle {


  private static ProjectConnection connect(Path gradleProject){
      return GradleConnector.newConnector()
              .forProjectDirectory(gradleProject.toFile())
              .connect();
  }


 private static void compile(Path gradleProject) {

      ProjectConnection connection = connect(gradleProject);

      try {
          // Configure the build
          BuildLauncher launcher = connection.newBuild();
          launcher.forTasks("classes");
          launcher.setStandardOutput(System.out);
          launcher.setStandardError(System.err);
          // Run the build
          launcher.run();
      } finally {
          // Clean up
          connection.close();

      }

  }

  public static List<UppdateraCoordinate> getClasspath(Path gradleProject) {
      ProjectConnection connection = connect(gradleProject);

      try {
          compile(gradleProject);
      } catch (Exception e) {
          throw e;
      }

      try {
          IdeaProject project = connection.getModel(IdeaProject.class);

          Map<IdeaModule, DomainObjectSet<? extends IdeaDependency>> dependencies =
                  project
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

          return compileDependencies.entrySet()
                  .stream()
                  .flatMap(e -> e.getValue().stream().map(UppdateraCoordinate::of))
                  .collect(Collectors.toList());

      } finally {
          connection.close();
      }

  }









}