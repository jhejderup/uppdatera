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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static guru.nidi.graphviz.model.Factory.*;

public class GenerateMavenCG2 {


    private static String buildClasspath(String mavenCoordinate){
        File[] artifacts = Maven.resolver().resolve(mavenCoordinate).withTransitivity().asFile();
        List<File> arts = Arrays.asList(artifacts);
        ArrayList<File> artlst = new ArrayList<>(arts);
        List<String> jars = artlst.stream().map(s -> s.getAbsolutePath()).collect(Collectors.toList());
        return String.join(":", new ArrayList<>(jars));

    }


    public static void main(String[] args) throws WalaException, CallGraphBuilderCancelException, IOException {

        final GraphvizCmdLineEngine engine = new GraphvizCmdLineEngine();

        engine.setDotOutputFile(args[0],args[1]);

        try {
           List<ResolvedCall> cg =  WalaCallgraphConstructor.build( buildClasspath(args[1]));
            Graph g = graph().directed().with(cg.stream().map(
                    call -> node(WalaCallgraphConstructor.convertToUFI(call.source).toString())
                            .link(to(node(WalaCallgraphConstructor.convertToUFI(call.target).toString())))
            ).toArray(LinkSource[]::new));
            Graphviz.useEngine(engine);
            Graphviz.fromGraph(g).render(Format.JSON);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
