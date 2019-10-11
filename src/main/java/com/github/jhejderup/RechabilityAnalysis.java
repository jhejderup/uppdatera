package com.github.jhejderup;

import com.github.jhejderup.data.ModuleClasspath;
import com.github.jhejderup.data.type.MavenResolvedCoordinate;
import com.github.jhejderup.generator.WalaCallgraphConstructor;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.MethodReference;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.stream.Collectors;

public class RechabilityAnalysis {

  public static String toUppdateraFunctionString(MethodReference ref) {
    return ref.getDeclaringClass().getName().toString() + "/" + ref
        .getSelector();
  }

  public static ClassLoaderReference getClassLoader(MethodReference m) {
    return m.getDeclaringClass().getClassLoader();
  }

  public static void main(String[] args) {
    ///
    /// How does it work?
    ///
    /// We fill the project-based classes in the Application Loader
    /// We fill the dependencies in the Extension Loader

    String PROJECT_CLASSES = "target/classes";
    String PROJECT_DEPS = "target/dependency";

    try {
      var project = new MavenResolvedCoordinate("", "", "",
          Path.of(PROJECT_CLASSES));
      var dependencies = Files.find(Paths.get(PROJECT_DEPS), Integer.MAX_VALUE,
          (filePath, fileAttr) -> fileAttr.isRegularFile() && filePath
              .toString().endsWith(".jar")).map(
          j -> new MavenResolvedCoordinate("", "", "", Paths.get(j.toUri())))
          .collect(Collectors.toList());

      //Create Classpath
      var classpath = new ModuleClasspath(project, Optional.of(dependencies));

      //Create call graph
      var cg = WalaCallgraphConstructor.build(classpath, args[0]);

      //Resolve call graph into a CHA call graph
      var CHACallgraph = WalaCallgraphConstructor.makeCHA(cg);

      var reverseEdges = CHACallgraph.stream().filter(
          call -> !getClassLoader(call.target)
              .equals(ClassLoaderReference.Primordial)).map(
          rs -> toUppdateraFunctionString(rs.target) + " "
              + toUppdateraFunctionString(rs.source));

      Files.write(Paths.get(args[0], "cg.txt"),
          (Iterable<String>) reverseEdges::iterator);

    } catch (Exception e) {
      e.printStackTrace();
    }

  }

}
