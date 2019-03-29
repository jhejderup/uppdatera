package com.github.jhejderup;


import com.github.jhejderup.data.MavenCoordinate;
import com.github.jhejderup.diff.ArtifactDiff;
import com.github.jhejderup.wala.WalaCallgraphConstructor;
import com.ibm.wala.ipa.callgraph.CallGraphBuilderCancelException;
import com.ibm.wala.util.WalaException;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;


public class App {


    public static void main(String[] args) throws IOException, TimeoutException, InterruptedException, WalaException, CallGraphBuilderCancelException {

        ArtifactDiff
                .diff(new MavenCoordinate("com.squareup.okhttp3", "okhttp", "3.13.1"),
                        new MavenCoordinate("com.squareup.okhttp3", "okhttp", "3.14.0")
                ).forEach(m -> m.forEach((k,v) -> {
                   // System.out.println(k);
              //      v.stream().forEach(System.out::println);
        }));
    }


}
