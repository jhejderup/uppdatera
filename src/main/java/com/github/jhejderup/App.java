package com.github.jhejderup;


import com.github.jhejderup.data.MavenCoordinate;
import com.github.jhejderup.diff.ArtifactDiff;

import java.io.IOException;
import java.util.concurrent.TimeoutException;


public class App {


    public static void main(String[] args) throws IOException, TimeoutException, InterruptedException {


        ArtifactDiff
                .of(new MavenCoordinate("com.squareup.okhttp3", "okhttp", "3.13.1"),
                        new MavenCoordinate("com.squareup.okhttp3", "okhttp", "3.14.0")
                ).forEach(mp -> mp.forEach((k, v) -> {
                    System.out.println(k);
                    v.stream().forEach(System.out::println);
                })
        );

    }


}
