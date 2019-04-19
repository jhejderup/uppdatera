package com.github.jhejderup;


import com.github.jhejderup.data.callgraph.WalaCallGraph;
import com.github.jhejderup.data.type.MavenResolvedCoordinate;
import com.github.jhejderup.generator.WalaCallgraphConstructor;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.jboss.shrinkwrap.resolver.api.maven.MavenResolvedArtifact;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;



public class GenerateMavenCG2 {

    private static Logger logger = LoggerFactory.getLogger(GenerateMavenCG2.class);


    private static List<MavenResolvedCoordinate> buildClasspath(String mavenCoordinate) {
        logger.info("Building analyzedClasspath of {}", mavenCoordinate);
        MavenResolvedArtifact[] artifacts = Maven.resolver().resolve(mavenCoordinate).withTransitivity().asResolvedArtifact();
        List<MavenResolvedArtifact> arts = Arrays.asList(artifacts);
        List<MavenResolvedCoordinate> path = arts.stream().map(MavenResolvedCoordinate::of).collect(Collectors.toList());
        logger.info("The analyzedClasspath of {} is {} ", mavenCoordinate, path);
        return path;

    }


    public static void main(String[] args) {
        logger.info("Constructing generator of package {}", args[1]);
        try {
            List<MavenResolvedCoordinate> path = buildClasspath(args[1]);
            logger.info("Building generator using generator....");
            WalaCallGraph cg =  WalaCallgraphConstructor.build(path);
            WalaUFIAdapter wrapped_cg = WalaUFIAdapter.wrap(cg);
            logger.info("Call graph construction done!");
            logger.info("Save it to file");
            wrapped_cg.toFile(args[0],args[1] + ".dot");
        } catch (Exception e) {
            logger.error("An exception occurred for {}",args[1], e);
        }
    }
}
