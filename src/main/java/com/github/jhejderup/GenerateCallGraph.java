package com.github.jhejderup;

import com.github.jhejderup.connectors.GradleBuild;
import com.github.jhejderup.connectors.MavenBuild;
import com.github.jhejderup.generator.WalaCallgraphConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Function;
import java.util.stream.Collectors;

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

        var tempRepository = Paths.get(args[0]);


        var modules = isGradleProject(tempRepository) ? GradleBuild.resolveClasspath(tempRepository) :
                MavenBuild.resolveClasspath(Path.of(tempRepository.toString(), "pom.xml"));


        var callgraphMap = modules.stream()
                .peek(m -> logger.info("Building call graph for {}", m.project))
                .collect(Collectors.toMap(Function.identity(), WalaCallgraphConstructor::build));


        callgraphMap.forEach((m, cg) -> {
            try {
                var fw = new FileWriter(m.project.jarPath.getParent() + "/functions.txt");

                cg.rawGraph.forEach(n -> {
                    try {
                        fw.write(n.toString());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });

                fw.close();

            } catch (IOException e) {
                e.printStackTrace();

            } finally {

            }

        });


        logger.info("Callgraph construction for project modules done!");


    }


}
