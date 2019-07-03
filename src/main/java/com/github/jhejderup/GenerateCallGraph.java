package com.github.jhejderup;

import com.github.jhejderup.connectors.GradleBuild;
import com.github.jhejderup.connectors.MavenBuild;
import com.github.jhejderup.generator.WalaCallgraphConstructor;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class GenerateCallGraph {

    private static Logger logger = LoggerFactory.getLogger(GenerateCallGraph.class);

    private static boolean isMavenProject(Path repository) {
        var pomFile = Paths.get(repository.toString(), "pom.xml");
        return Files.exists(pomFile);
    }


    private static boolean isGradleProject(Path repository) {
        var gradleFile = Paths.get(repository.toString(), "build.gradle");
        return Files.exists(gradleFile);
    }


    public static void main(String[] args) {

        //0. Seperate input
        var readPaths = args[0].split(" ");

        var pomXML = readPaths[0];
        var appJAR = readPaths[1];

        logger.info("pom.xml located at {}", pomXML);
        logger.info("application jar located at {}", appJAR);

        //2. Resolve dependencies of pom.xml files
        try {
            var depz = Maven.resolver()
                    .loadPomFromFile(pomXML)
                    .importCompileAndRuntimeDependencies()
                    .resolve()
                    .withTransitivity().asFile();



            var classpath = Stream.concat(Stream.of(appJAR),Arrays.stream(depz).map(f -> f.toString()))
                    .collect(Collectors.joining(";"));

            logger.info(classpath);


        } catch (Exception e) {
            logger.error("Failed for {} with exception: {}",pomXML, e);
        }
    }


}
