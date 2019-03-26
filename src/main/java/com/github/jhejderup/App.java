package com.github.jhejderup;


import gumtree.spoon.AstComparator;
import gumtree.spoon.diff.Diff;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.PathFilter;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.omg.PortableInterceptor.SYSTEM_EXCEPTION;
import spoon.reflect.declaration.*;
import spoon.support.reflect.declaration.CtConstructorImpl;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;


public class App {


    public static void main(String[] args) throws IOException, GitAPIException {

        /// Learned from this example, not only specify source-jar but also sources afterwards
        /// Maven.resolver().resolve("G:A:test-jar:tests:V").withTransitivity().asFile();

        File newJar = Maven.resolver()
                .resolve("com.squareup.okhttp3:okhttp:java-source:sources:3.14.0")
                .withoutTransitivity()
                .asSingleFile();

        File oldJar = Maven.resolver()
                .resolve("com.squareup.okhttp3:okhttp:java-source:sources:3.13.1")
                .withoutTransitivity()
                .asSingleFile();


        Path destPath = Paths.get("/Users/jhejderup/Desktop/uppdatera/okhttp");

        if (!Files.notExists(destPath)) {
            //always delete it
            Files.walk(destPath)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    //     .peek(System.out::println)
                    .forEach(File::delete);
        }

        Files.createDirectories(destPath);

        ZipFile archieveOld = new ZipFile(oldJar);

        archieveOld.stream().forEach(entry -> {
            Path entryDest = destPath.resolve(entry.getName());
            try {
                if (entry.isDirectory()) {
                    Files.createDirectory(entryDest);
                } else {
                    Files.copy(archieveOld.getInputStream(entry), entryDest);
                }
            } catch (Exception e) {
                System.out.println("Errors");
            }
        });


        Git git = Git.init()
                .setDirectory(destPath.toFile())
                .call();

        git.add().addFilepattern(".").call();
        RevCommit left = git.commit().setMessage("3.13.1").call();


        ZipFile archieveNew = new ZipFile(newJar);
        archieveNew.stream().forEach(entry -> {
            Path entryDest = destPath.resolve(entry.getName());
            try {
                if (entry.isDirectory() && Files.notExists(entryDest)) {
                    Files.createDirectory(entryDest);
                } else if (!entry.isDirectory()) {
                    Files.copy(archieveNew.getInputStream(entry), entryDest, StandardCopyOption.REPLACE_EXISTING);
                }
            } catch (Exception e) {
                System.out.println(e);
            }
        });

        git.add().addFilepattern(".").call();
        RevCommit right = git.commit().setMessage("3.14.0").call();

        //Start diff

        ObjectReader reader = git.getRepository().newObjectReader();

        CanonicalTreeParser leftTreeIter = new CanonicalTreeParser();
        leftTreeIter.reset(reader, left.getTree());


        CanonicalTreeParser rightTreeIter = new CanonicalTreeParser();
        rightTreeIter.reset(reader, right.getTree());


        List<DiffEntry> diffs = git.diff().setNewTree(leftTreeIter).setOldTree(rightTreeIter).call();

        diffs.stream()
                .filter(entry -> entry.getOldPath().endsWith(".java"))
                .filter(entry -> !entry.getOldPath().contains("/src/test/"))
                .forEach(k -> {
                    try {

                        System.out.println("Processing: " + k.getOldPath());

                        //Painful bloat to fetch the file in the repo

                        //old
                        TreeWalk leftTree = new TreeWalk(git.getRepository());
                        leftTree.addTree(left.getTree());
                        leftTree.setRecursive(true);
                        leftTree.setFilter(PathFilter.create(k.getOldPath()));


                        //new
                        TreeWalk rightTree = new TreeWalk(git.getRepository());
                        rightTree.addTree(right.getTree());
                        rightTree.setRecursive(true);
                        rightTree.setFilter(PathFilter.create(k.getOldPath()));

                        //Move next to retrieve the file

                        if (rightTree.next() && leftTree.next()) {

                            //Yet more bloat to read file content
                            ObjectId objectIdOld = leftTree.getObjectId(0);
                            ObjectLoader leftLoader = git.getRepository().open(objectIdOld);

                            ObjectId objectIdNew = rightTree.getObjectId(0);
                            ObjectLoader rightLoader = git.getRepository().open(objectIdNew);

                            //Finally gumtree time!
                            AstComparator diff = new AstComparator();

                            CtType<?> astLeft = diff.getCtType(new String(leftLoader.getBytes(), "utf-8"));
                            CtType<?> astRight = diff.getCtType(new String(rightLoader.getBytes(), "utf-8"));

                            Diff editScript = diff.compare(astLeft,astRight);

                            editScript
                                    .getAllOperations()
                                    .stream()
                                    .forEach(op -> {
                                           //changed (inserted/deleted/updated) element
                                           //getDstNode will always be null
                                        if(op.getDstNode() == null){
                                            if (op.getSrcNode() != null) {
                                                CtElement parent = op.getSrcNode();
                                                while (parent.getParent() != null && !(parent.getParent() instanceof CtClass)) {
                                                    parent = parent.getParent();
                                                }
                                                    //parent instanceof CtConstructor
                                                if(parent instanceof CtMethod){

                                                    System.out.println(((CtMethod) parent).getSignature());
                                                    System.out.println(" change made ");
                                                    System.out.println(op.getSrcNode().getPath());
                                                    System.out.println(op);
                                                    System.out.println("---");
                                                } else {


//                                                    System.out.println("---");
//                                                    System.out.println(op.getClass().getSimpleName());
//                                                    System.out.println(parent.getClass());
//                                                    System.out.println("~~~~~");
//                                                    System.out.println(op.getSrcNode().getClass());
//                                                     System.out.println(parent);
//                                                    System.out.println("---");

                                                }


                                            } else {

                                                // some elements are only in the gumtree for having a clean diff
                                                // but not in the Spoon metamodel
                                            }

                                        } else {
                                            // the new version of the node (only for update)
                                            CtElement dstParent = op.getDstNode();
                                            while (dstParent.getParent() != null && !(dstParent.getParent() instanceof CtClass)) {
                                                dstParent = dstParent.getParent();
                                            }

                                            CtElement srcParent = op.getSrcNode();
                                            while (srcParent.getParent() != null && !(srcParent.getParent() instanceof CtClass)) {
                                                srcParent = srcParent.getParent();
                                            }

                                            if(dstParent instanceof CtMethod && srcParent instanceof CtMethod) {
                                                System.out.println(((CtMethod) srcParent).getSignature());
                                                System.out.println(" update to in the next version");
                                                System.out.println(((CtMethod) dstParent).getSignature());
                                                System.out.println(" change was:");
                                                System.out.println(op);

                                            }

                                        }
                                    });


                        }





                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });













    }
}

