package com.github.jhejderup;


import com.github.gumtreediff.actions.ActionGenerator;
import com.github.gumtreediff.actions.model.Action;
import com.github.gumtreediff.actions.model.Move;
import com.github.gumtreediff.gen.Generators;
import com.github.gumtreediff.matchers.Matcher;
import com.github.gumtreediff.matchers.Matchers;
import com.github.gumtreediff.tree.ITree;
import com.github.gumtreediff.tree.TreeContext;
import com.github.jhejderup.diff.ChangeClassifier;
import gumtree.spoon.AstComparator;
import gumtree.spoon.diff.Diff;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.LogCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.lib.*;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.PathFilter;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.omg.PortableInterceptor.SYSTEM_EXCEPTION;
import spoon.reflect.code.CtStatement;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtType;

import java.awt.color.CMMException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

public class App {


    public static void main(String[] args) {

        String c1 = "" + "class X {" + "public void foo() {" + " int x = 0;" + "}" + "};";

        String c2 = "" + "class X  {" + "public void foo()   {" + "" + "}" + "};";

        AstComparator diff = new AstComparator();
        CtType<?> left = diff.getCtType(c1);
        CtType<?> right = diff.getCtType(c2);


        System.out.println(left.getMethods());
        System.out.println(right.getMethods());
        Diff editScript = diff.compare(left, right);

        editScript.getRootOperations()
                .stream()
                .forEach(op -> {
                    CtElement src = op.getSrcNode();
                    CtElement dst = op.getDstNode();
                    if(src != null && dst != null) {
                       System.out.println(src.getPath());
                        System.out.println(dst.getPath());
                        System.out.println(op.getAction().getClass().getSimpleName());
                    }

                    if (src != null && dst == null) {
                        System.out.println("No dst");
                        System.out.println(src.getPath());

                        System.out.println(src.getParent().getClass().getSimpleName());

                    }
                });
        System.out.println(editScript);



    }



//    public static void main(String[] args) throws GitAPIException, IOException {
//
////        File jarNew = Maven.resolver()
////                .resolve("com.squareup.okhttp3:okhttp:3.14.0")
////                .withoutTransitivity()
////                .asSingleFile();
////
////        File jarOld = Maven.resolver()
////                .resolve("com.squareup.okhttp3:okhttp:3.13.1")
////                .withoutTransitivity()
////                .asSingleFile();
//
//
////        Git git = Git.cloneRepository()
////                .setURI("https://github.com/square/retrofit")
////                .call();
//
//        FileRepositoryBuilder repositoryBuilder = new FileRepositoryBuilder();
//        Repository repository = repositoryBuilder.setGitDir(new File("/Users/jhejderup/Desktop/uppdatera/retrofit/.git"))
//                .readEnvironment() // scan environment GIT_* variables
//                .findGitDir() // scan up the file system tree
//                .setMustExist(true)
//                .build();
//
//        Git git = Git.wrap(repository);
//
//
//
//        List<Ref> refs = git.tagList().call();
//        RevWalk walk = new RevWalk(git.getRepository());
//
//
//        refs.stream().forEach(ref -> {
//            try {
//                RevCommit commit = walk.parseCommit(ref.getObjectId());
//                walk.reset();
//
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//
//        });
//
//        //2.4.0 -> 7158698314daa138e993fac6a590ed19d78a8599
//        //2.5.0 -> 40621bf538f6d48af182350adc880406c53dc67c
//        //2.0.2 -> fd2f2e24a9e3aae3f7f6a832da3bb6fc22362577
//
//
//        ObjectReader reader = git.getRepository().newObjectReader();
//
//        ObjectId oldid = repository.resolve("7158698314daa138e993fac6a590ed19d78a8599");
//        RevCommit commitOld = walk.parseCommit(oldid);
//        walk.reset();
//
//        CanonicalTreeParser oldTreeIter = new CanonicalTreeParser();
//        oldTreeIter.reset(reader, commitOld.getTree());
//
//        ObjectId newid = repository.resolve("40621bf538f6d48af182350adc880406c53dc67c");
//        RevCommit commitNew = walk.parseCommit(newid);
//        walk.reset();
//
//        CanonicalTreeParser newTreeIter = new CanonicalTreeParser();
//        newTreeIter.reset(reader, commitNew.getTree());
//
//        List<DiffEntry> diffs = git.diff().setNewTree(newTreeIter).setOldTree(oldTreeIter).call();
//
//        diffs.stream()
//                .filter(entry -> entry.getOldPath().endsWith(".java"))
//                .filter(entry -> !entry.getOldPath().contains("/src/test/"))
//                .forEach(k -> {
//
//                    try {
//
//                        System.out.println(k.getOldPath());
//                        //old
//                        TreeWalk oldTree = new TreeWalk(repository);
//                        oldTree.addTree(commitOld.getTree());
//                        oldTree.setRecursive(true);
//                        oldTree.setFilter(PathFilter.create(k.getOldPath()));
//
//
//
//                        //new
//                        TreeWalk newTree = new TreeWalk(repository);
//                        newTree.addTree(commitNew.getTree());
//                        newTree.setRecursive(true);
//                        newTree.setFilter(PathFilter.create(k.getOldPath()));
//
//
//
//                        if (newTree.next() && oldTree.next()) {
//
//                            ObjectId objectIdOld = oldTree.getObjectId(0);
//                            ObjectLoader loaderOld = repository.open(objectIdOld);
//
//                            ObjectId objectIdNew = newTree.getObjectId(0);
//                            ObjectLoader loaderNew = repository.open(objectIdNew);
//
//
//                            AstComparator diff = new AstComparator();
//
//
//
//                            CtType<?> astLeft = diff.getCtType(new String(loaderNew.getBytes(), "utf-8"));
//                            CtType<?> astRight = diff.getCtType(new String(loaderOld.getBytes(), "utf-8"));
//
//
//
//                            System.out.println(diff.compare(astLeft,astRight));
//
//
//                            if (astRight != null && astRight.getMethods().size() > 0){
//                                Map<String,CtMethod> methodLookup = astRight
//                                        .getMethods()
//                                        .stream()
//                                        .collect(Collectors.toMap(CtMethod::getSignature, CtMethod::clone));
//
//                                astLeft.getMethods().stream().forEach(methodLeft -> {
//
//                                if(methodLookup.containsKey(methodLeft.getSignature())) {
//                                    CtMethod methodRight = methodLookup.get(methodLeft.getSignature());
//                          //          System.out.println(methodLeft.getSignature());
//                            //        System.out.println(methodRight.getSignature());
//                                    Diff editScript = diff.compare(methodLeft, methodRight);
//
//                                     //   System.out.println(editScript.getRootOperations());
//                                    try {
//                                        editScript.getRootOperations().stream().forEach(System.out::println);
//                                    } catch (Exception e) {
//                                 //       System.out.println(e);
//                                    }
//
//                                }
//
//
//                            });
//
//                        }
//                        }
//
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                    }
//
//                });
//    }

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
