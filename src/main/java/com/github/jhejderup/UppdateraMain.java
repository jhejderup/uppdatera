package com.github.jhejderup;

import com.github.jhejderup.data.ModuleClasspath;
import com.github.jhejderup.data.type.MavenResolvedCoordinate;
import com.github.jhejderup.generator.WalaCallgraphConstructor;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.jboss.shrinkwrap.resolver.api.maven.ScopeType;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;

public class UppdateraMain {

    public static void main(String[] args) {

        assert args.length == 2;

        var subject = args[0];
        var searchFunction = args[1];

        //Fetch all dependencies
        var depJARs = Maven.configureResolver()
                .workOffline()
                .loadPomFromFile(subject + "/pom.xml")
                .importDependencies(
                        ScopeType.TEST,
                        ScopeType.SYSTEM,
                        ScopeType.COMPILE,
                        ScopeType.IMPORT,
                        ScopeType.RUNTIME,
                        ScopeType.PROVIDED
                ).resolve()
                .withTransitivity()
                .asResolvedArtifact();


        var resolvedDependencies = Arrays.stream(depJARs)
                .map(MavenResolvedCoordinate::of)
                .collect(Collectors.toList());

        //Fetch project jar
        var project = new MavenResolvedCoordinate("", "", "", Path.of(subject, "uppdatera.jar"));

        //Create Classpath
        var classpath = new ModuleClasspath(project, Optional.of(resolvedDependencies));

        //Create call graph
        var cg = WalaCallgraphConstructor.build(classpath);

        //Resolve all calls
        var resolvedCalls = WalaCallgraphConstructor.resolveCalls(cg.rawGraph);

        resolvedCalls.stream().map(k -> k.source.getDescriptor());


    }
}
