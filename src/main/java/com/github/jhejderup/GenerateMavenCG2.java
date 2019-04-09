package com.github.jhejderup;

import com.github.jhejderup.data.ResolvedCall;
import com.github.jhejderup.wala.WalaCallgraphConstructor;
import com.ibm.wala.ipa.callgraph.CallGraphBuilderCancelException;
import com.ibm.wala.util.WalaException;
import guru.nidi.graphviz.engine.Format;
import guru.nidi.graphviz.engine.Graphviz;
import guru.nidi.graphviz.engine.GraphvizCmdLineEngine;
import guru.nidi.graphviz.model.Graph;
import guru.nidi.graphviz.model.LinkSource;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static guru.nidi.graphviz.model.Factory.*;

public class GenerateMavenCG2 {

    private static Logger logger = LoggerFactory.getLogger(GenerateMavenCG2.class);


    private static String buildClasspath(String mavenCoordinate){
        logger.info("Building classpath of {}", mavenCoordinate);
        File[] artifacts = Maven.resolver().resolve(mavenCoordinate).withTransitivity().asFile();
        List<File> arts = Arrays.asList(artifacts);
        ArrayList<File> artlst = new ArrayList<>(arts);
        List<String> jars = artlst.stream().map(s -> s.getAbsolutePath()).collect(Collectors.toList());
        String path = String.join(":", new ArrayList<>(jars));
        logger.info("The classpath of {} is {} ",mavenCoordinate, path);
        return path;

    }


    public static void main(String[] args) {

        logger.info("Constructing callgraph of package {}", args[1]);

        final GraphvizCmdLineEngine engine = new GraphvizCmdLineEngine();

        engine.setDotOutputFile(args[0],args[1]);

        try {
            String path = buildClasspath(args[1]);
            logger.info("Building callgraph using wala....");
           List<ResolvedCall> cg =  WalaCallgraphConstructor.build(path);
            logger.info("Call graph construction done!");
            logger.info("Convert it to graphviz");
            Graph g = graph().directed().with(cg.stream().map(
                    call -> node(WalaCallgraphConstructor.convertToUFI(call.source).toString())
                            .link(to(node(WalaCallgraphConstructor.convertToUFI(call.target).toString())))
            ).toArray(LinkSource[]::new));
            logger.info("Convertion done, save call graph to {}", args[1]);
            Graphviz.useEngine(engine);
            Graphviz.fromGraph(g).render(Format.PLAIN).toString();
        } catch (Exception e) {
            logger.error("An exception occurred for {}",args[1], e);
        }
    }
}
