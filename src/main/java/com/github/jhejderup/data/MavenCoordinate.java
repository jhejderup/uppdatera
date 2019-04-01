package com.github.jhejderup.data;

import java.io.Serializable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class MavenCoordinate implements Serializable {

    private final static Pattern pattern = Pattern.compile("m2\\/repository\\/?(?<group>.*)\\/(?<artifact>[^\\/]*)\\/(?<version>[^\\/]*)\\/([^\\/]*).jar");

    public final String artifactId;
    public final String groupId;
    public final String version;

    public MavenCoordinate(String groupId, String artifactId, String version) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
    }


    public static MavenCoordinate of(String jarPath) {
        Matcher matcher = pattern.matcher(jarPath);
        matcher.find();
        return new MavenCoordinate(
                matcher.group("group").replace('/', '.'),
                matcher.group("artifact"),
                matcher.group("version")
        );
    }


}
