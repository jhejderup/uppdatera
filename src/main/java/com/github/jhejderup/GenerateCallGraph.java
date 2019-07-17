package com.github.jhejderup;

import com.github.jhejderup.data.ModuleClasspath;
import com.github.jhejderup.data.type.MavenResolvedCoordinate;
import com.github.jhejderup.generator.WalaCallgraphConstructor;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;

public class GenerateCallGraph {

    private static Logger logger = LoggerFactory.getLogger(GenerateCallGraph.class);

    public static void main(String[] args) {

        //0. Seperate input
        var pomXML = args[0];
        logger.info("pom.xml located at {}", pomXML);
      
        //2. Resolve dependencies of pom.xml files
        try {
            var depzFiles = Maven.resolver()
                    .loadPomFromFile(pomXML)
                    .importCompileAndRuntimeDependencies()
                    .resolve()
                    .withTransitivity().asFile();
            
            Arrays.stream(depz).forEach(System.out::println);
            

        } catch (Exception e) {
            logger.error("Failed for {} with exception: {}", pomXML, e);
            e.printStackTrace();
        }
    }


}
