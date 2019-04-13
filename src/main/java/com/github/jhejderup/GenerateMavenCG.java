package com.github.jhejderup;

public class GenerateMavenCG {


//    private static String buildClasspath(String mavenCoordinate){
//        File[] artifacts = Maven.resolver().resolve(mavenCoordinate).withTransitivity().asFile();
//        List<File> arts = Arrays.asList(artifacts);
//        ArrayList<File> artlst = new ArrayList<>(arts);
//        List<String> jars = artlst.stream().map(s -> s.getAbsolutePath()).collect(Collectors.toList());
//        return String.join(":", new ArrayList<>(jars));
//
//    }
//
//
//    public static void main(String[] args) throws WalaException, CallGraphBuilderCancelException, IOException {
//
//
//        //  pkgs.add("com.facebook.presto:presto-plugin-toolkit:jar:0.215"); NOT WORKING
//        //  pkgs.add("com.facebook.presto:presto-cli:jar:0.206"); NOT WORKING
////   pkgs.add("org.apache.pulsar:pulsar-presto-distribution:jar:2.3.0");
//        //    pkgs.add("org.apache.gora:gora-core:jar:0.8"); NOT WORKING
//        // pkgs.add("org.apache.camel:camel-pubnub:jar:2.23.0"); NOT WORKING
//
//        ArrayList<String> pkgs = new ArrayList<>();
//       // pkgs.add("com.squareup.pagerduty:pagerduty-incidents:jar:1.0.1"); DONE
//      //  pkgs.add("com.pubnub:pubnub-gson:jar:4.21.0"); DONE
//       // pkgs.add("com.mapbox.mapboxsdk:mapbox-sdk-services:jar:4.5.0"); DONE
//      //  pkgs.add("de.125m125.kt:ktapi-retrofit:jar:1.0.0"); DONE
//     //   pkgs.add("de.125m125.kt:ktapi-smartCache:jar:1.0.0"); DONE
//      //  pkgs.add("com.spotify:apollo-api-impl:jar:1.11.0"); DONE
//     //   pkgs.add("com.nytimes.android:filesystem3:jar:3.1.1"); DONE
//        //  pkgs.add("com.squareup.wire:wire-gson-support:jar:2.2.0"); DONE
//     //   pkgs.add("com.squareup.okhttp3:okcurl:jar:3.14.0");
//     //   pkgs.add("org.trellisldp:trellis-namespaces:jar:0.8.1");
//        //pkgs.add("org.symphonyoss.s2.common:S2-common-core:jar:0.1.34");
//      //  pkgs.add("com.github.korthout:cantis:jar:0.1");
//        //pkgs.add("org.seleniumhq.selenium:selenium-remote-driver:jar:3.141.5");
//        //pkgs.add("org.jbpm:jbpm-persistence-jpa:jar:6.0.0.CR1"); WALA ERR
//
//        pkgs.add("io.graphenee:gx-core:jar:0.1.1");
//      //  pkgs.add("io.deepsense:deepsense-api_2.11:jar:1.4.1"); //WALA ERR
//
//        pkgs.add("uk.co.nichesolutions.presto:presto-benchmark:jar:0.143-CUSTOM");
//
//        final GraphvizCmdLineEngine engine = new GraphvizCmdLineEngine();
//
//
//        pkgs
//                .stream()
//                .peek(s -> engine.setDotOutputFile("/Users/jhejderup/Desktop/callgraphs",s))
//                .map(GenerateMavenCG::buildClasspath)
//                .map(analyzedClasspath -> {
//                    try {
//                        return WalaCallgraphConstructor.build(analyzedClasspath);
//                    } catch (Exception e) {
//                        e.printStackTrace();
//                        return null;
//                    }
//                })
//                .filter(Objects::nonNull)
//                .map(cg -> graph().directed().with(cg.stream().map(
//                        call -> node(WalaCallgraphConstructor.convertToUFI(call.source).toString())
//                                .link(to(node(WalaCallgraphConstructor.convertToUFI(call.target).toString())))
//                        ).toArray(LinkSource[]::new)
//                )).forEach(graph -> {
//                    Graphviz.useEngine(engine);
//                   Graphviz.fromGraph(graph).render(Format.JSON);
//                   System.out.println("Callgraph generated check output folder!");
//                });
//
//
//
//
//
//
//
//    }
}
