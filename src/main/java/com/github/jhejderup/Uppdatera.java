package com.github.jhejderup;


import com.github.jhejderup.connectors.GradleBuild;
import com.github.jhejderup.data.diff.ArtifactDiff;
import com.github.jhejderup.data.type.MavenCoordinate;
import com.github.jhejderup.data.type.MavenResolvedCoordinate;
import com.github.jhejderup.diff.Differ;
import com.github.jhejderup.generator.WalaCallgraphConstructor;
import com.ibm.wala.properties.WalaProperties;

import java.nio.file.Paths;
import java.util.List;
import java.util.Properties;
import java.util.stream.Stream;


public class Uppdatera {


    public static void main(String[] args)
            throws Exception {

        assert Paths.get(args[0]).isAbsolute();

        var leftCoord = MavenCoordinate.of(args[1]);
        var rightCoord = MavenCoordinate.of(args[2]);


        var clientClasspath = GradleBuild.makeClasspath(Paths.get(args[0]));

        Stream.of(clientClasspath)
                .map(WalaCallgraphConstructor::build)
                .map(s -> s.mappings())
                .forEach(s -> s.keySet().stream().forEach(System.out::println));

//
//        List<MavenResolvedCoordinate> mavenClasspath = MavenBuild
//                .buildClasspath(Paths.get("/Users/jhejderup/Desktop/uppdatera/pom.xml"));
//
//
//        Stream.of(mavenClasspath)
//                .map(WalaCallgraphConstructor::build)
//                .map(WalaUFIAdapter::wrap)
//                .map(s -> s.mappings())
//                .forEach(s -> s.keySet().stream().forEach(System.out::println));
//
////

       var editScript = Differ.artifact(leftCoord, rightCoord);

        editScript.mappings()
                .entrySet()
                .forEach(e -> System.out.println(e.getValue().getSignature() + "<-:-:->" + e.getKey()));
    }


}
