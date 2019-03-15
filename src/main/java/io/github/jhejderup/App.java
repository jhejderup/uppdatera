package io.github.jhejderup;


import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.jboss.shrinkwrap.resolver.api.maven.MavenArtifactInfo;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.StringReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Arrays;
import java.util.stream.IntStream;
import java.util.stream.Stream;


public class App {

    private final static DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    private final static XPath xPath = XPathFactory.newInstance().newXPath();
    private final static HttpClient httpClient = HttpClient.newBuilder().version(HttpClient.Version.HTTP_2).build();


    private static HttpRequest createRequest(String coordinates) {
        return HttpRequest
                .newBuilder()
                .uri(URI.create("http://repo1.maven.org/maven2/" + coordinates + "/maven-metadata.xml"))
                .GET()
                .build();
    }

    private static Stream<String> parse(String xmlStr) {
        try {
            var builder = factory.newDocumentBuilder();
            var doc = builder.parse(new InputSource(new StringReader(xmlStr)));
            var versions = (NodeList) xPath.compile("//versions/version").evaluate(doc, XPathConstants.NODESET);
            var groupId = (Node) xPath.compile("//groupId").evaluate(doc, XPathConstants.NODE);
            var artifactId = (Node) xPath.compile("//artifactId").evaluate(doc, XPathConstants.NODE);

            return IntStream.range(0, versions.getLength())
                    .mapToObj(versions::item)
                    .map(Node::getTextContent)
                    .map(ver -> groupId.getTextContent() + ":" + artifactId.getTextContent() + ":" + ver);

        } catch (Exception e) {
            return Stream.empty();
        }
    }

    private static Stream<String> sendRequest(HttpRequest request) {
        try {
            return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenApply(HttpResponse::body)
                    .thenApply(App::parse)
                    .get();
        } catch (Exception e) {
            return Stream.empty();
        }

    }

    public static void main(String[] args) {

        Stream
                .of("com.squareup.okhttp3:okcurl")
                .map(s -> s.replace(".", "/").replace(":", "/"))
                .map(App::createRequest)
                .flatMap(App::sendRequest)
                .forEach(System.out::println);


    }
}
