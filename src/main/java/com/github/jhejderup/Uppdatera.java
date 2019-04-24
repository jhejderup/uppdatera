package com.github.jhejderup;


import com.github.jhejderup.connectors.GradleBuild;
import com.github.jhejderup.connectors.MavenBuild;
import com.github.jhejderup.data.UpdatePair;
import com.github.jhejderup.data.type.MavenCoordinate;
import com.github.jhejderup.diff.Differ;
import com.github.jhejderup.generator.WalaCallgraphConstructor;
import net.lingala.zip4j.exception.ZipException;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;


public class Uppdatera {

    private final static DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    private final static XPath xPath = XPathFactory.newInstance().newXPath();
    private final static HttpClient httpClient = HttpClient.newBuilder().version(HttpClient.Version.HTTP_2).build();
    private static Logger logger = LoggerFactory.getLogger(Uppdatera.class);


    private static Stream<MavenCoordinate> fetchReleasesFromMavenCentral(MavenCoordinate coordinate) {
        return Stream.of(coordinate)
                .map(Uppdatera::createRequest)
                .flatMap(Uppdatera::sendRequest);
    }


    private static Stream<UpdatePair> findUpdates(MavenCoordinate current) {
        return Stream.of(current)
                .flatMap(Uppdatera::fetchReleasesFromMavenCentral)
                .filter(c -> c.version.isHigherThan(current.version))
                .sorted(MavenCoordinate::compareTo)
                .map(newRelease -> new UpdatePair(current, newRelease));
    }

    private static HttpRequest createRequest(MavenCoordinate coord) {
        return HttpRequest
                .newBuilder()
                .uri(URI.create(String.format("http://repo1.maven.org/maven2/%s/%s/maven-metadata.xml",
                        coord.groupId.replace(".", "/"), coord.artifactId)))
                .GET()
                .build();
    }

    private static Stream<MavenCoordinate> parseResponse(String xmlStr) {
        try {
            var builder = factory.newDocumentBuilder();
            var doc = builder.parse(new InputSource(new StringReader(xmlStr)));
            var versions = (NodeList) xPath.compile("//versions/version").evaluate(doc, XPathConstants.NODESET);
            var groupId = (Node) xPath.compile("//groupId").evaluate(doc, XPathConstants.NODE);
            var artifactId = (Node) xPath.compile("//artifactId").evaluate(doc, XPathConstants.NODE);

            return IntStream.range(0, versions.getLength())
                    .mapToObj(versions::item)
                    .map(Node::getTextContent)
                    .map(ver -> new MavenCoordinate(groupId.getTextContent(), artifactId.getTextContent(), ver));

        } catch (Exception e) {
            return Stream.empty();
        }
    }

    private static Stream<MavenCoordinate> sendRequest(HttpRequest request) {
        try {
            return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenApply(HttpResponse::body)
                    .thenApply(Uppdatera::parseResponse)
                    .get();
        } catch (Exception e) {
            return Stream.empty();
        }

    }

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

    public static void main(String[] args) {

        try {
            Path tempRepository = Files.createTempDirectory("uppdatera-git");
            logger.info("Cloning Github repository '{}'", args[0]);
            var git = Git.cloneRepository()
                    .setURI(String.format("https://github.com/%s.git", args[0]))
                    .setDirectory(tempRepository.toFile())
                    .call();
            logger.info("Successfully cloned to {}", tempRepository.toFile());

            if (isMavenOrGradleProject(tempRepository)) {
                logger.info("Project is identified as a Gradle/Maven Project");


                var modules = isGradleProject(tempRepository) ? GradleBuild.resolveClasspath(tempRepository) :
                        MavenBuild.resolveClasspath(Path.of(tempRepository.toString(), "pom.xml"));

                var dependencyUpdateMap = modules.stream()
                        .filter(m -> m.dependencies.isPresent())
                        .collect(Collectors.toMap(Function.identity(),
                                k -> k.dependencies.get().stream()
                                        .map(d -> findUpdates(d).findFirst())
                                        .flatMap(Optional::stream)));

                var callgraphMap = modules.stream()
                        .peek(m -> logger.info("Building call graph for {}", m.project))
                        .collect(Collectors.toMap(Function.identity(), WalaCallgraphConstructor::build));

                logger.info("Callgraph construction for project modules done!");
                dependencyUpdateMap.entrySet().stream().forEach(e -> {

                    logger.info("Updates for module {} ", e.getKey().project);

                    e.getValue().peek(p -> logger.info("Dependency {}:{}:({} -> {})", p.left.groupId, p.left.artifactId,
                            p.left.version.getOriginalString(),
                            p.right.version.getOriginalString()))
                            .map(p -> {
                                try {
                                    return Differ.artifact(p.left, p.right);
                                } catch (IOException e1) {
                                    e1.printStackTrace();
                                } catch (TimeoutException e1) {
                                    e1.printStackTrace();
                                } catch (InterruptedException e1) {
                                    e1.printStackTrace();
                                } catch (ZipException e1) {
                                    e1.printStackTrace();
                                }
                                return null;
                            })
                            .filter(Objects::nonNull)
                            .forEach(diff -> {

                                var callgraph = callgraphMap.get(e.getKey());

                                logger.info("These are the changes between {} - {}", diff.left, diff.right);
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
                                                                .peek(System.out::println)
                                                                .forEach(m -> callgraph.walk(m));

                                                        System.out.println("which contains the following changes in the updated version");
                                                        edit.get(k).stream().forEach(System.out::println);
                                                    });
                                        });
                            });

                });


            } else {
                logger.error("Not a Gradle or Maven Project");
                return;
            }


        } catch (IOException | GitAPIException e) {
            logger.error("An exception occurred when cloning the '{}' repository", args[0], e);

        }

    }
}
