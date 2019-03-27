package com.github.jhejderup.diff;

import com.github.jhejderup.data.MavenCoordinate;
import gumtree.spoon.AstComparator;
import gumtree.spoon.diff.operations.Operation;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.zeroturnaround.exec.ProcessExecutor;
import spoon.reflect.declaration.CtConstructor;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtExecutable;
import spoon.reflect.declaration.CtMethod;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ArtifactDiff {


    public static Stream<Map<String, List<Operation>>> of(MavenCoordinate coordLeft, MavenCoordinate coordRight) throws IOException, TimeoutException, InterruptedException {
        //1. download, unzip sources
        Path left = fetchSource(coordLeft);
        Path right = fetchSource(coordRight);

        //2. diff file level
        return diff(left, right)
                .map(tmpFile -> {
                    String baseFile = tmpFile.replace(left.toString(), "");
                    try {
                        //3. diff AST level
                        return javaSourceFileDiff(
                                baseFile,
                                new File(left.toString() + baseFile),
                                new File(right.toString() + baseFile)
                        );
                    } catch (Exception e) {
                        return null;
                    }
                })
                .filter(Objects::nonNull);

    }

    public static Map<String, List<Operation>> javaSourceFileDiff(String OriginalName, File left, File right) throws Exception {

        AstComparator diff = new AstComparator();
        gumtree.spoon.diff.Diff editScript = diff.compare(left, right);
        return editScript
                .getRootOperations()
                .stream()
                .filter(op -> isChangeInMethod(op.getSrcNode()))
                .collect(Collectors.toMap(
                        op -> OriginalName + ":" + ((CtExecutable) getAssociatedMethod(op.getSrcNode()).get()).getSignature(),
                        op -> {
                            List ops = new ArrayList<Operation>();
                            ops.add(op);
                            return ops;
                        }, (l1, l2) -> {
                            l1.addAll(l2);
                            return l1;
                        }));

    }

    private static boolean isChangeInMethod(CtElement node) {
        return getAssociatedMethod(node).isPresent();
    }

    private static Optional<CtElement> getAssociatedMethod(CtElement node) {
        if (isMethod(node) || isConstructor(node)) {
            return Optional.of(node);
        }
        try {
            CtMethod parentMethod = node.getParent(CtMethod.class);
            CtConstructor parentConstructor = node.getParent(CtConstructor.class);
            if (parentMethod == null && parentConstructor == null)
                return Optional.empty();
            else if (parentMethod != null && parentConstructor == null)
                return Optional.of(parentMethod);
            else
                return Optional.of(parentConstructor);
        } catch (NullPointerException e) {
            return Optional.empty();
        }
    }

    private static boolean isMethod(CtElement element) {
        return element instanceof CtMethod;
    }

    private static boolean isConstructor(CtElement element) {
        return element instanceof CtConstructor;
    }

    //
    // File ArtifactDiff
    //

    private static Path fetchSource(MavenCoordinate coordinate) throws IOException {
        File jarFile = Maven.resolver()
                .resolve(coordinate.groupId + ":" + coordinate.artifactId + ":java-source:sources:" + coordinate.version)
                .withoutTransitivity()
                .asSingleFile();
        return unzip(jarFile);
    }

    private static Path unzip(final File zipFilePath) throws IOException {

        Path unzipLocation = Files.createTempDirectory("uppdateratempz");

        if (!(Files.exists(unzipLocation))) {
            Files.createDirectories(unzipLocation);
        }
        try (ZipInputStream zipInputStream = new ZipInputStream(new FileInputStream(zipFilePath))) {
            ZipEntry entry = zipInputStream.getNextEntry();
            while (entry != null) {
                Path filePath = Paths.get(unzipLocation.toString(), entry.getName());
                if (!entry.isDirectory()) {
                    unzipFiles(zipInputStream, filePath);
                } else {
                    Files.createDirectories(filePath);
                }

                zipInputStream.closeEntry();
                entry = zipInputStream.getNextEntry();
            }
        }

        return unzipLocation;
    }

    private static void unzipFiles(final ZipInputStream zipInputStream, final Path unzipFilePath) throws IOException {

        try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(unzipFilePath.toAbsolutePath().toString()))) {
            byte[] bytesIn = new byte[1024];
            int read = 0;
            while ((read = zipInputStream.read(bytesIn)) != -1) {
                bos.write(bytesIn, 0, read);
            }
        }

    }

    private static String gitDiff(Path left, Path right) throws InterruptedException, TimeoutException, IOException {
        return new ProcessExecutor()
                .command("git", "diff", "--no-index", "--name-status", left.toString(), right.toString())
                .readOutput(true).execute()
                .outputString();
    }

    private static Stream<String> diff(Path left, Path right) throws InterruptedException, TimeoutException, IOException {
        String raw = gitDiff(left, right);
        return Arrays.stream(raw.split("\n"))
                .map(l -> l.split("\t"))
                .filter(ar -> ar[1].endsWith(".java"))
                .filter(ar -> !ar[1].contains("/src/tests"))
                .filter(ar -> ar[0].contains("M"))
                .map(ar -> ar[1]); //TODO: parse into a data structure instead (also handle del/moves)
    }
}
