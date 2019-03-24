package com.github.jhejderup;


import com.github.jhejderup.wala.WalaCallgraphConstructor;
import com.ibm.wala.ipa.callgraph.CallGraphBuilderCancelException;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class App {

    public static void main(String[] args) throws ClassHierarchyException, CallGraphBuilderCancelException, IOException {

        File[] artifacts = Maven.resolver().resolve("com.squareup.okhttp3:okcurl:3.14.0").withTransitivity().asFile();
        List<File> arts = Arrays.asList(artifacts);
        ArrayList<File> artlst = new ArrayList<>(arts);
        List<String> jars = artlst.stream().map(s -> s.getAbsolutePath()).collect(Collectors.toList());

        String classpath = String.join(":", new ArrayList<>(jars));

        WalaCallgraphConstructor.build(classpath);

    }
}
