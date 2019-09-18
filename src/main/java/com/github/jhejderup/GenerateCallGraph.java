package com.github.jhejderup;

import com.github.jhejderup.data.ModuleClasspath;
import com.github.jhejderup.data.type.MavenResolvedCoordinate;
import com.github.jhejderup.generator.WalaCallgraphConstructor;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Optional;
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

        //0. Seperate input
        var readPaths = args[0].split(" ");

        var pomXML = readPaths[0];
        var appJAR = readPaths[1];
        logger.info("pom.xml located at {}", pomXML);
        logger.info("application jar located at {}", appJAR);

        //2. Resolve dependencies of pom.xml files
        try {
            var depzFiles = Maven.resolver()
                    .loadPomFromFile(pomXML)
                    .importCompileAndRuntimeDependencies()
                    .resolve()
                    .withTransitivity().asFile();

            var app = new MavenResolvedCoordinate("", "", "", Path.of(appJAR));

            var depz = Arrays.stream(depzFiles).map(k -> k.toPath())
                    .map(d -> new MavenResolvedCoordinate("", "", "", d))
                    .collect(Collectors.toList());


            var classpath = new ModuleClasspath(app, Optional.of(depz));


            var callgraph = WalaCallgraphConstructor.build(classpath);


            var fileWriter = new PrintWriter(Path.of(pomXML).getParent().toString() + "/uppdatera-calls.txt");


            callgraph.rawGraph.forEach(n -> {
                if (WalaCallgraphConstructor.isUppdateraScope(n.getMethod())) {
                    fileWriter.println(n.getMethod().getSignature());
                    logger.info(n.getMethod().getSignature());
                }

            });

            fileWriter.close();


        } catch (Exception e) {
            logger.error("Failed for {} with exception: {}", pomXML, e);
            e.printStackTrace();
        }
    }


}
