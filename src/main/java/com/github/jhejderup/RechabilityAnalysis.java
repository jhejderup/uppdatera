package com.github.jhejderup;

import com.github.jhejderup.data.ModuleClasspath;
import com.github.jhejderup.data.type.MavenResolvedCoordinate;
import com.github.jhejderup.generator.WalaCallgraphConstructor;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.types.ClassLoaderReference;
import org.jboss.shrinkwrap.resolver.api.maven.embedded.BuiltProject;
import org.jboss.shrinkwrap.resolver.api.maven.embedded.EmbeddedMaven;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class RechabilityAnalysis {

    public static int resolveDependencies() {

        BuiltProject b = EmbeddedMaven
                .forProject("pom.xml")
                .useDefaultDistribution()
                .setGoals("dependency:copy-dependencies", "-DincludeScope=runtime")
                .build();

        return b.getMavenBuildExitCode();

    }

    public static int buildJARfile() {

        BuiltProject b = EmbeddedMaven
                .forProject("pom.xml")
                .useDefaultDistribution()
                .setGoals("jar:jar", "-Djar.finalName=uppdatera")
                .build();

        return b.getMavenBuildExitCode();

    }

    public static Set<UppdateraMethod> getFunctions() throws IOException {
        return Files.lines(Paths.get("functions.txt"))
                .map(l -> new UppdateraMethod(l, ClassLoaderReference.Extension))
                .collect(Collectors.toSet());
    }

    public static String toUppdateraFunctionString(IMethod m) {
        var ref = m.getReference();
        return ref.getDeclaringClass().getName().toString() + "/" + ref.getSelector();

    }

    public static Boolean isApplicationOrExtension(IMethod m) {
        return getClassLoader(m).equals(ClassLoaderReference.Application) ||
                getClassLoader(m).equals(ClassLoaderReference.Extension);
    }

    public static ClassLoaderReference getClassLoader(IMethod m) {
        return m.getReference().getDeclaringClass().getClassLoader();
    }

    // Generic function to merge two sets in Java
    public static <T> Set<T> mergeSets(Set<T> a, Set<T> b) {
        return Stream.of(a, b)
                .flatMap(x -> x.stream())
                .collect(Collectors.toSet());
    }

    public static void main(String[] args) {
        ///
        /// How does it work?
        ///
        /// We fill the project-based classes in the Application Loader
        /// We fill the dependencies in the Extension Loader

        var buildCode = buildJARfile();
        assert buildCode == 0;
        var depCode = resolveDependencies();
        assert depCode == 0;

        String PROJECT_JAR = "target/uppdatera.jar";
        String PROJECT_DEPS = "target/dependency";

        try {
            var functions = getFunctions();
            var project = new MavenResolvedCoordinate("", "", "", Path.of(PROJECT_JAR));
            var dependencies = Files.find(Paths.get(PROJECT_DEPS),
                    Integer.MAX_VALUE,
                    (filePath, fileAttr) -> fileAttr.isRegularFile() && filePath.toString().endsWith(".jar"))
                    .map(j -> new MavenResolvedCoordinate("", "", "", Paths.get(j.toUri())))
                    .collect(Collectors.toList());

            //Create Classpath
            var classpath = new ModuleClasspath(project, Optional.of(dependencies));

            //Create call graph
            var cg = WalaCallgraphConstructor.build(classpath);

            //Resolve function calls
            var resolvedCalls = WalaCallgraphConstructor.makeCHA(cg.rawGraph);

            Function<IMethod, UppdateraMethod> toUppdatera =
                    m -> new UppdateraMethod(toUppdateraFunctionString(m), getClassLoader(m));


            //Flip edges and create a reverse cg
            var reverseCgMap = resolvedCalls
                    .stream()
                    .collect(Collectors.toMap(
                            c -> toUppdatera.apply(c.target),
                            c -> Set.of(toUppdatera.apply(c.source)),
                            (c1, c2) -> mergeSets(c1, c2)
                            )
                    );

            //Set of all functions in the call graph
            var cgFns = resolvedCalls.stream()
                    .flatMap(call -> Stream.of(call.target, call.source))
                    .map(m -> toUppdatera.apply(m))
                    .collect(Collectors.toSet());



            //1. Check that all functions exists in the CG!
            var missingFns = functions
                    .stream()
                    .filter(fn -> !cgFns.contains(fn))
                    .collect(Collectors.toList());

            var percentage = (((float) missingFns.size() / (float) functions.size()) * 100);

            System.out.println("We are missing " +
                    missingFns.size() +
                    " functions (" +
                    percentage +
                    "%)");

            System.out.println("These are not present:");
            //TODO: write to file
            missingFns
                    .stream()
                    .map(fn -> fn.name)
                    .forEach(System.out::println);


            //2. Search algorithm (per function)
            // - only visit new keys! (never the same)
            // - stop criteria: next key belongs to Application Loader


        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    static class UppdateraMethod {

        public final String name;
        public final ClassLoaderReference classLoader;

        public UppdateraMethod(String name, ClassLoaderReference classLoader) {
            this.name = name;
            this.classLoader = classLoader;
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, classLoader);
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this)
                return true;

            if (obj == null || obj.getClass() != getClass())
                return false;

            UppdateraMethod m = (UppdateraMethod) obj;
            return Objects.equals(name, m.name) && Objects.equals(classLoader, m.classLoader);
        }
    }
}
