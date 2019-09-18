package com.github.jhejderup;

import com.github.jhejderup.connectors.GradleBuild;
import com.github.jhejderup.connectors.MavenBuild;
import com.github.jhejderup.data.type.MavenCoordinate;
import com.github.jhejderup.diff.Differ;
import com.github.jhejderup.generator.WalaCallgraphConstructor;
import net.lingala.zip4j.exception.ZipException;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;
import java.io.IOException;
import java.net.http.HttpClient;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import java.util.stream.Collectors;

public class Uppdatera2 {

    private final static DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    private final static XPath xPath = XPathFactory.newInstance().newXPath();
    private final static HttpClient httpClient = HttpClient.newBuilder().version(HttpClient.Version.HTTP_2).build();
    private static Logger logger = LoggerFactory.getLogger(Uppdatera.class);


    private static boolean isMavenProject(Path repository) {
        var pomFile = Paths.get(repository.toString(), "pom.xml");
        return Files.exists(pomFile);
    }


    private static boolean isGradleProject(Path repository) {
        var gradleFile = Paths.get(repository.toString(), "build.gradle");
        return Files.exists(gradleFile);
    }

    private static boolean isMavenOrGradleProject(Path repository) {
        return isGradleProject(repository) || isMavenProject(repository);
    }

    public static void main(String[] args) throws InterruptedException, ZipException, TimeoutException, GitAPIException {

        try {
            Path tempRepository = Files.createTempDirectory("uppdatera-git");
            //     Path tempRepository = Path.of("/Users/jhejderup/Downloads/rxjava-bug");
            logger.info("Cloning Github repository '{}'", args[0]);
            var git = Git.cloneRepository()
                    .setURI(String.format("https://github.com/%s.git", args[0]))
                    .setDirectory(tempRepository.toFile())
                    .call();
            logger.info("Successfully cloned to {}", tempRepository.toFile());

            git.checkout().setName(args[1]).call();
            logger.info("Successfully checkedout commit {}", args[1]);

            if (isMavenOrGradleProject(tempRepository)) {
                logger.info("Project is identified as a Gradle/Maven Project");


                var modules = isGradleProject(tempRepository) ? GradleBuild.resolveClasspath(tempRepository) :
                        MavenBuild.resolveClasspath(Path.of(tempRepository.toString(), "pom.xml"));

                var callgraphMap = modules.stream()
                        .peek(m -> logger.info("Building call graph for {}", m.project))
                        .collect(Collectors.toMap(Function.identity(), WalaCallgraphConstructor::build));

                logger.info("Callgraph construction for project modules done!");
                callgraphMap.forEach((mod, callgraph) -> {
                    callgraph.rawGraph.forEach(System.out::println);
                });


                var diff = Differ.artifact(
                        new MavenCoordinate("com.google.code.gson", "gson", "2.2.4"),
                        new MavenCoordinate("com.google.code.gson", "gson", "2.8.5"));

                logger.info("These are the changes between {} - {}", diff.left, diff.right);

                callgraphMap.forEach((mod, callgraph) -> {
                    diff.editScript
                            .stream().map(c -> c.methodDiffs)
                            .flatMap(Optional::stream)
                            .forEach(edit -> {
                                edit.keySet()
                                        .stream()
                                        .filter(m -> callgraph.mappings().containsKey(diff.convertToUFI(m)))
                                        .forEach(k -> {

                                            System.out.println("In the rawGraph, we make a call to " + diff.convertToUFI(k));

                                            var method = callgraph.mappings().get(diff.convertToUFI(k));

                                            var cgnodes = callgraph.rawGraph.getNodes(method.getReference());

                                            cgnodes.stream()
                                                    .filter(n -> WalaCallgraphConstructor.isApplication(n.getMethod().getDeclaringClass()))
                                                    .map(n -> n.getMethod())
                                                    .forEach(System.out::println);
                                            //  .forEach(m -> callgraph.walk(m));

                                            System.out.println("which contains the following changes in the updated version");
                                            //   if(k.getSignature().contains("toString"))
                                            edit.get(k).stream().forEach(System.out::println);
                                        });
                            });

                });

            } else {
                logger.error("Not a Gradle or Maven Project");
                return;
            }


        } catch (IOException e) {
            logger.error("An exception occurred when cloning the '{}' repository", args[0], e);

        }

    }
}
