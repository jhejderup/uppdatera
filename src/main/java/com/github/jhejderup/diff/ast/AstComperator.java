/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.jhejderup.diff.ast;

import gumtree.spoon.builder.SpoonGumTreeBuilder;
import gumtree.spoon.diff.Diff;
import gumtree.spoon.diff.DiffImpl;
import spoon.SpoonModelBuilder;
import spoon.compiler.SpoonResource;
import spoon.compiler.SpoonResourceHelper;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtType;
import spoon.reflect.factory.Factory;
import spoon.reflect.factory.FactoryImpl;
import spoon.support.DefaultCoreFactory;
import spoon.support.StandardEnvironment;
import spoon.support.compiler.VirtualFile;
import spoon.support.compiler.jdt.JDTBasedSpoonCompiler;

import java.io.File;

public class AstComperator {
  // For the moment, let's create a factory each type we get a type.
  // Sharing the factory produces a bug when asking the path of different types
  // (>1)
  // private final Factory factory;

  static {
    // default 0.3
    // it seems that default value is really bad
    // 0.1 one failing much more changes
    // 0.2 one failing much more changes
    // 0.3 one failing test_t_224542
    // 0.4 fails for issue31
    // 0.5 fails for issue31
    // 0.6 OK
    // 0.7 1 failing
    // 0.8 2 failing
    // 0.9 two failing tests with more changes
    // see GreedyBottomUpMatcher.java in Gumtree
    System.setProperty("gumtree.match.bu.sim", "0.6");

    // default 2
    // 0 is really bad for 211903 t_224542 225391 226622
    // 1 is required for t_225262 and t_213712 to pass
    System.setProperty("gumtree.match.gt.minh", "1");

    // default 1000
    // 0 fails
    // 1 fails
    // 10 fails
    // 100 OK
    // 1000 OK
    // see AbstractBottomUpMatcher#SIZE_THRESHOD in Gumtree
    // System.setProperty("gumtree.match.bu.size","10");
    // System.setProperty("gt.bum.szt", "100
    // 0");
  }

  private final String[] classPath;

  public AstComperator(String... classPath) {
    super();
    this.classPath = classPath;
  }

  public static void main(String[] args) throws Exception {
    if (args.length != 2) {
      System.out.println("Usage: DiffSpoon <file_1>  <file_2>");
      return;
    }

    final Diff result = new AstComperator()
        .compare(new File(args[0]), new File(args[1]));
    System.out.println(result.toString());
  }

  protected Factory createFactory() {
    Factory factory = new FactoryImpl(new DefaultCoreFactory(),
        new StandardEnvironment());
    factory.getEnvironment().setSourceClasspath(classPath);
    factory.getEnvironment().setCommentEnabled(false);
    return factory;
  }

  /**
   * compares two java files
   */
  public Diff compare(File f1, File f2) throws Exception {
    return this.compare(getCtType(f1), getCtType(f2));
  }

  /**
   * compares two snippet
   */
  public Diff compare(String left, String right) {
    return compare(getCtType(left), getCtType(right));
  }

  /**
   * compares two AST nodes
   */
  public Diff compare(CtElement left, CtElement right) {
    final SpoonGumTreeBuilder scanner = new SpoonGumTreeBuilder();
    return new DiffImpl(scanner.getTreeContext(), scanner.getTree(left),
        scanner.getTree(right));
  }

  public CtType getCtType(File file) throws Exception {

    SpoonResource resource = SpoonResourceHelper.createResource(file);
    return getCtType(resource);
  }

  public CtType getCtType(SpoonResource resource) {
    Factory factory = createFactory();
    factory.getModel().setBuildModelIsFinished(false);
    SpoonModelBuilder compiler = new JDTBasedSpoonCompiler(factory);
    compiler.getFactory().getEnvironment().setLevel("OFF");
    compiler.addInputSource(resource);
    compiler.build();

    if (factory.Type().getAll().size() == 0) {
      return null;
    }

    // let's first take the first type.
    CtType type = factory.Type().getAll().get(0);
    // Now, let's ask to the factory the type (which it will set up the
    // corresponding
    // package)
    return factory.Type().get(type.getQualifiedName());
  }

  public CtType<?> getCtType(String content) {
    VirtualFile resource = new VirtualFile(content, "/test");
    return getCtType(resource);
  }

}
