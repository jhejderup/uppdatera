package com.github.jhejderup.data;


import com.github.jhejderup.data.type.MavenResolvedCoordinate;

import java.io.Serializable;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

public final class ModuleClasspath implements Serializable {

    public final MavenResolvedCoordinate project;
    public final Optional<List<MavenResolvedCoordinate>> dependencies;

    public ModuleClasspath(MavenResolvedCoordinate project, Optional<List<MavenResolvedCoordinate>>  dependencies) {
        this.project = project;
        this.dependencies = dependencies;
    }

    public List<MavenResolvedCoordinate> getCompleteClasspath() {
        return Stream.concat(Stream.of(project),
                dependencies.map(l -> l.stream()).orElse(Stream.empty())
        ).collect(toList());
    }
}
