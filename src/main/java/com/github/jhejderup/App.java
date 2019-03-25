package com.github.jhejderup;


import japicmp.cmp.JApiCmpArchive;
import japicmp.cmp.JarArchiveComparator;
import japicmp.cmp.JarArchiveComparatorOptions;
import japicmp.config.Options;
import japicmp.model.JApiClass;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class App {


    public static void main(String[] args) throws IOException {

        File jarNew = Maven.resolver()
                .resolve("com.squareup.okhttp3:okhttp:3.14.0")
                .withoutTransitivity()
                .asSingleFile();

        File jarOld = Maven.resolver()
                .resolve("com.squareup.okhttp3:okhttp:3.13.1")
                .withoutTransitivity()
                .asSingleFile();

        System.out.println(jarNew);
        System.out.println(jarOld);

        Options options = Options.newDefault();
        options.setIgnoreMissingClasses(true);
        options.setOutputOnlyModifications(true);


        JarArchiveComparatorOptions comparatorOptions = JarArchiveComparatorOptions.of(options);
        JarArchiveComparator jarArchiveComparator = new JarArchiveComparator(comparatorOptions);
        List<JApiClass> jApiClasses = jarArchiveComparator.compare(new JApiCmpArchive(jarOld, "3.13.1"), new JApiCmpArchive(jarNew, "3.14.0"));



        jApiClasses.stream()
                .filter(klass -> klass.getChangeStatus().name() != "UNCHANGED")
                .flatMap(klass -> klass.getMethods().stream())
                .filter(method -> method.getChangeStatus().name() == "MODIFIED")
                .forEach(System.out::println);


    }

//    public static void main(String[] args) throws ClassHierarchyException, CallGraphBuilderCancelException, IOException {
//
//        File[] artifacts = Maven.resolver().resolve("com.squareup.okhttp3:okcurl:3.14.0").withTransitivity().asFile();
//        List<File> arts = Arrays.asList(artifacts);
//        ArrayList<File> artlst = new ArrayList<>(arts);
//        List<String> jars = artlst.stream().map(s -> s.getAbsolutePath()).collect(Collectors.toList());
//
//        String classpath = String.join(":", new ArrayList<>(jars));
//
//        WalaCallgraphConstructor.build(classpath);
//
//    }
}
