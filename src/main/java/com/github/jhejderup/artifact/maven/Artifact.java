/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.jhejderup.artifact.maven;

import com.github.jhejderup.artifact.Package;
import net.lingala.zip4j.core.ZipFile;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

public final class Artifact implements Package {

  private static Logger     logger = LoggerFactory.getLogger(Artifact.class);
  private final  Coordinate coord;

  public Artifact(Coordinate coord) {
    this.coord = coord;

  }

  @Override
  public Optional<Path> getSource() {
    try {
      var jarFile = Maven.resolver().resolve(
          coord.groupId + ":" + coord.artifactId + ":java-source:sources:"
              + coord.version).withoutTransitivity().asSingleFile();
      logger.info(
          "[ShrinkWrap] Downloaded " + jarFile.toString() + " for " + coord);

      Path unzipLocation = Files.createTempDirectory("uppdateratempz");
      ZipFile zipFile = new ZipFile(jarFile);
      zipFile.extractAll(unzipLocation.toString());
      logger.info("[ShrinkWrap] Extracted " + jarFile.toString() + " to "
          + unzipLocation);
      return Optional.of(unzipLocation);

    } catch (Exception e) {
      e.printStackTrace();
      logger.error(
          "[ShrinkWrap] Failed to download and unzip sources for " + coord
              .toString());
      return Optional.empty();
    }
  }

  @Override
  public Optional<Path> getBinary() {
    try {
      var jarFile = Maven.resolver()
          .resolve(coord.groupId + ":" + coord.artifactId + ":" + coord.version)
          .withoutTransitivity().asSingleFile();
      logger.info(
          "[ShrinkWrap] Downloaded " + jarFile.toString() + " for " + coord);
      return Optional.of(jarFile.toPath());
    } catch (Exception e) {
      logger.error(
          "[ShrinkWrap] Failed to download jar file for" + coord.toString());
      return Optional.empty();
    }
  }
}
