package com.github.jhejderup;


import com.github.jhejderup.connectors.GradleBuild;
import com.github.jhejderup.connectors.MavenBuild;
import com.github.jhejderup.generator.WalaCallgraphConstructor;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;


public class Uppdatera {

    private static Logger logger = LoggerFactory.getLogger(Uppdatera.class);



    private static boolean isMavenProject(Path repository) {
        var pomFile = Paths.get(repository.toString(),"pom.xml");
                return Files.exists(pomFile);
    }


    private static boolean isGradleProject(Path repository) {
        var gradleFile = Paths.get(repository.toString(),"build.gradle");
        return Files.exists(gradleFile);
    }

    private static boolean isMavenOrGradleProject(Path repository){
        return isGradleProject(repository) || isMavenProject(repository);
    }




    public static void main(String[] args)  {

        try {
            Path tempRepository = Files.createTempDirectory("uppdatera-git");
            logger.info("Cloning Github repository '{}'", args[0]);
            var git = Git.cloneRepository()
                    .setURI(String.format("https://github.com/%s.git", args[0]))
                    .setDirectory(tempRepository.toFile())
                    .call();
            logger.info("Successfully cloned to {}", tempRepository.toFile());

            if(isMavenOrGradleProject(tempRepository)){
                logger.info("Project is identified as a Gradle/Maven Project");
                var modules = isGradleProject(tempRepository) ? GradleBuild.resolveClasspath(tempRepository) :
                        MavenBuild.resolveClasspath(Path.of(tempRepository.toString(),"pom.xml"));

                modules.stream()
                        .map(WalaCallgraphConstructor::build)
                        .forEach(cg ->cg.mappings().keySet().stream().forEach(System.out::println));

            } else {
                logger.error("Not a Gradle or Maven Project");
                return;
            }




            

        } catch (IOException | GitAPIException e) {
            logger.error("An exception occurred when cloning the '{}' repository", args[0], e);

        }

    }
}
