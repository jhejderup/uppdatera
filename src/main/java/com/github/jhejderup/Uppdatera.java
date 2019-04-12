package com.github.jhejderup;


import com.github.jhejderup.connectors.Gradle;
import com.github.jhejderup.connectors.Maven;
import com.github.jhejderup.data.diff.FileDiff;
import com.github.jhejderup.data.type.MavenResolvedCoordinate;
import com.github.jhejderup.data.type.MavenCoordinate;
import com.github.jhejderup.diff.ArtifactDiff;
import com.github.jhejderup.generator.WalaCallgraphConstructor;
import gumtree.spoon.diff.operations.Operation;
import spoon.reflect.declaration.CtExecutable;


import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;


public class Uppdatera {


    public static void main(String[] args)
            throws Exception {

        assert Paths.get(args[0]).isAbsolute();

        MavenCoordinate leftCoord = MavenCoordinate.of(args[1]);
        MavenCoordinate rightCoord =  MavenCoordinate.of(args[2]);


//        List<MavenResolvedCoordinate> clientClasspath = Gradle
//                .getClasspath(Paths.get(args[0]));
//
//        Stream.of(clientClasspath)
//                .map(WalaCallgraphConstructor::build)
//                .map(WalaUFIAdapter::wrap)
//                .map(s -> s.mappings())
//                .forEach(s -> s.keySet().stream().forEach(System.out::println));


        List<MavenResolvedCoordinate> mavenClasspath = Maven
                .getClasspath(Paths.get("/Users/jhejderup/Desktop/uppdatera/pom.xml"));


        Stream.of(mavenClasspath)
                .map(WalaCallgraphConstructor::build)
                .map(WalaUFIAdapter::wrap)
                .map(s -> s.mappings())
                .forEach(s -> s.keySet().stream().forEach(System.out::println));



        SpoonUFIAdapter classtable = SpoonUFIAdapter.withTransitive(leftCoord);


        ArtifactDiff
                .diff(leftCoord, rightCoord)
                .forEach(diff -> {
                    if(diff.fileDiff.type != FileDiff.Change.ADDITION && diff.fileDiff.type != FileDiff.Change.COPY){
                        if(diff.methodDiffs.isPresent()) {
                            Map<CtExecutable,List<Operation>> methods = diff.methodDiffs.get();
                             methods.keySet()
                                    .stream()
                                    .map(m -> classtable.convertToUFI(m))
                                    .forEach(UFI -> {
                                        System.out.println(UFI);
                                    });
                        }
                    }
                });




    }


}
