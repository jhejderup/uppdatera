package com.github.jhejderup;

import com.github.jhejderup.data.callgraph.ResolvedCall;
import com.github.jhejderup.data.callgraph.WalaCallGraph;
import com.github.jhejderup.data.type.MavenCoordinate;
import com.github.jhejderup.data.type.MavenResolvedCoordinate;
import com.github.jhejderup.generator.WalaCallgraphConstructor;
import guru.nidi.graphviz.engine.Format;
import guru.nidi.graphviz.engine.Graphviz;
import guru.nidi.graphviz.engine.GraphvizCmdLineEngine;
import guru.nidi.graphviz.model.Graph;
import guru.nidi.graphviz.model.LinkSource;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.jboss.shrinkwrap.resolver.api.maven.MavenResolvedArtifact;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static guru.nidi.graphviz.model.Factory.graph;
import static guru.nidi.graphviz.model.Factory.node;
import static guru.nidi.graphviz.model.Link.to;

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

        final GraphvizCmdLineEngine engine = new GraphvizCmdLineEngine();

        engine.setDotOutputFile(args[0],args[1]);

        try {
            List<MavenResolvedCoordinate> path = buildClasspath(args[1]);
            logger.info("Building generator using generator....");
           WalaCallGraph cg =  WalaCallgraphConstructor.build(path);
           WalaUFIAdapter wrapped_cg = WalaUFIAdapter.wrap(cg);
            logger.info("Call graph construction done!");
            logger.info("Convert it to graphviz");
            Graph g = graph().directed().with(WalaCallgraphConstructor.resolveCalls(wrapped_cg.callGraph.rawcg).stream().map(
                    call -> node(wrapped_cg.convertToUFI(call.source).toString())
                            .link(to(node(wrapped_cg.convertToUFI(call.target).toString())))
            ).toArray(LinkSource[]::new));
            logger.info("Convertion done, save call graph to {}", args[1]);
            Graphviz.useEngine(engine);
            Graphviz.fromGraph(g).render(Format.PLAIN).toString();
        } catch (Exception e) {
            logger.error("An exception occurred for {}",args[1], e);
        }
    }
}
