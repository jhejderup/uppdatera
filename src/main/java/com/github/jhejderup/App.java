package com.github.jhejderup;


import com.github.jhejderup.diff.JavaSourceDiff;
import gumtree.spoon.diff.operations.Operation;
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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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


                            Map<String, List<Operation>> editMethods = JavaSourceDiff.editMethodScript(
                                    new String(leftLoader.getBytes(), "utf-8"),
                                    new String(rightLoader.getBytes(), "utf-8"));

                            editMethods.forEach((key,v) -> {
                                System.out.println(key);
                                v.stream().forEach(System.out::println);
                            });

                        }

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });













    }
}
