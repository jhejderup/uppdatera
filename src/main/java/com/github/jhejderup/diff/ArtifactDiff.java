package com.github.jhejderup.diff;

import com.github.jhejderup.data.FileDiff;
import com.github.jhejderup.data.MavenCoordinate;
import gumtree.spoon.AstComparator;
import gumtree.spoon.diff.operations.Operation;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.zeroturnaround.exec.ProcessExecutor;
import spoon.reflect.declaration.*;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.TimeoutException;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ArtifactDiff {


    public static Stream<Map<String, List<Operation>>> diff(MavenCoordinate coordLeft, MavenCoordinate coordRight) throws IOException, TimeoutException, InterruptedException {
        //1. download, unzip sources
        Path leftFolder = fetchSource(coordLeft);
        Path rightFolder = fetchSource(coordRight);

        buildUFItable(coordLeft);
        buildUFItable(coordRight);

        //2. diffFiles file level
        return diffFiles(leftFolder, rightFolder)
                .filter(ArtifactDiff::isImpactKind)
                .filter(ArtifactDiff::isJavaFile)
                .filter(ArtifactDiff::isNotTestFile)
                .map(fileDiff -> {
                    try {
                        return diffAST(fileDiff);
                    } catch (Exception e) {
                        e.printStackTrace();
                        System.out.println(fileDiff);
                        return null;
                    }
                }).filter(Objects::nonNull);

        //3.

    }

    private static boolean isImpactKind(FileDiff diff) {
        return diff.type == FileDiff.Change.DELETION ||
                diff.type == FileDiff.Change.RENAME ||
                diff.type == FileDiff.Change.MODIFICATION;
    }

    private static boolean isJavaFile(FileDiff diff) {
        Optional<Path> src = diff.source;
        if (src.isPresent())
            return src.get().toString().endsWith(".java");
        else
            return false;
    }

    private static boolean isNotTestFile(FileDiff diff) {
        Optional<Path> src = diff.source;
        if (src.isPresent())
            return !src.get().toString().contains("/src/test/");
        else
            return false;
    }


    private static Map<String, List<Operation>> diffAST(FileDiff fileDiff) throws Exception {
        assert (fileDiff.type != FileDiff.Change.ADDITION);
        assert (fileDiff.type != FileDiff.Change.COPY);

        AstComparator diff = new AstComparator();

        if (fileDiff.type == FileDiff.Change.DELETION ||
                fileDiff.type == FileDiff.Change.RENAME) {

            CtType ast = diff.getCtType(fileDiff.source.get().toFile());
            return diff.compare(ast, null)
                    .getAllOperations()
                    .stream()
                    .filter(op -> op.getSrcNode() != null) //due to non-spoon objects
                    .filter(op -> isChangeInMethod(op.getSrcNode()))
                    .collect(Collectors.toMap(
                            op -> ((CtExecutable) getTopLevelMethod(op.getSrcNode()).get()).getSignature(),
                            op -> {
                                List ops = new ArrayList<Operation>();
                                ops.add(op);
                                return ops;
                            }, (l1, l2) -> {
                                l1.addAll(l2);
                                return l1;
                            }));
        } else {

            gumtree.spoon.diff.Diff editScript = diff.compare(
                    fileDiff.source.get().toFile(),
                    fileDiff.destination.get().toFile());

            return editScript
                    .getRootOperations()
                    .stream()
                    .filter(op -> isChangeInMethod(op.getSrcNode()))
                    .collect(Collectors.toMap(
                            op -> ((CtExecutable) getTopLevelMethod(op.getSrcNode()).get()).getSignature(),
                            op -> {
                                List ops = new ArrayList<Operation>();
                                ops.add(op);
                                return ops;
                            }, (l1, l2) -> {
                                l1.addAll(l2);
                                return l1;
                            }));
        }

    }


    private static boolean isChangeInMethod(CtElement node) {
        return getTopLevelMethod(node).isPresent();
    }


    private static Optional<CtElement> getTopLevelMethod(CtElement node) {

        CtElement parent = node;
        Stack<CtElement> stack = new Stack<>();

        try {
            while (parent.getParent() != null) {
                stack.push(parent);
                parent = parent.getParent();
            }

            if (stack.size() > 0) {
                CtElement method = stack.pop();
                while (stack.size() > 0) {
                    if (isMethod(method) || isConstructor(method)) {
//                           System.out.println(
//                                 method.getParent(CtType.class).getQualifiedName().replace(".", "::") + "::" +
//                                       ((CtExecutable) method).getSignature().replace(".", "::"));
                        return Optional.of(method);
                    }
                    method = stack.pop();
                }
                return Optional.empty();
            } else {
                return Optional.empty();
            }
        } catch (Exception e) {
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

    private static void buildUFItable(MavenCoordinate coordinate) {
        File[] artifacts = Maven.resolver()
                .resolve(coordinate.groupId + ":" + coordinate.artifactId + ":" + coordinate.version)
                .withTransitivity()
                .asFile();
        Arrays.stream(artifacts)
                .map(artifactPath -> {
                    try {
                        JarFile jar = new JarFile(artifactPath.toString());
                        return new Object[]{artifactPath.toString(), jar};

                    } catch (IOException e) {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .flatMap(arr -> ((JarFile)arr[1]).stream().map(entry -> new Object[] {arr[0], entry} ))
                .filter(arr -> ((JarEntry)arr[1]).getName().endsWith(".class"))
                .map(arr ->  new Object[] {arr[0], ((JarEntry)arr[1]).getName().replaceAll("/", "\\.") })
                .map(arr ->  new Object[] {arr[0], ((String)arr[1]).substring(0, ((String)arr[1]).lastIndexOf('.'))})
                .map(arr -> {
                    MavenCoordinate coord = MavenCoordinate.getMavenCoordinateFromJAR((String) arr[0]);
                    return "mvn::"
                            + coord.groupId.replace(".","::") + "::"
                            + coord.artifactId + "::"
                            + coord.version + "::"
                            + ((String)arr[1]).replace(".","::");
                })
                .forEach(System.out::println);
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

    private static String invokeGitDiff(Path leftFolder, Path rightFolder) throws InterruptedException, TimeoutException, IOException {
        return new ProcessExecutor()
                .command("git", "diff", "--no-index", "--name-status", leftFolder.toString(), rightFolder.toString())
                .readOutput(true).execute()
                .outputString();
    }

    private static Stream<FileDiff> diffFiles(Path leftFolder, Path rightFolder) throws InterruptedException, TimeoutException, IOException {
        String raw = invokeGitDiff(leftFolder, rightFolder);

        return Arrays.stream(raw.split("\n"))
                .map(l -> l.split("\t"))
                .map(arr -> {
                    FileDiff.Change type = getChangeType(arr[0]);
                    if (arr.length == 3) {
                        return new FileDiff(
                                Optional.of(Paths.get(arr[1])),
                                Optional.of(Paths.get(arr[2])),
                                type);
                    } else {
                        switch (type) {
                            case MODIFICATION:
                                String filename = arr[1].replace(leftFolder.toString(), "");
                                return new FileDiff(
                                        Optional.of(Paths.get(arr[1])),
                                        Optional.of(Paths.get(rightFolder + filename)),
                                        getChangeType(arr[0]));
                            case DELETION:
                                return new FileDiff(
                                        Optional.of(Paths.get(arr[1])),
                                        Optional.empty(),
                                        getChangeType(arr[0]));
                            case ADDITION:
                                return new FileDiff(
                                        Optional.empty(),
                                        Optional.of(Paths.get(arr[1])),
                                        getChangeType(arr[0]));
                            default:
                                return null;
                        }
                    }
                })
                .filter(Objects::nonNull);
    }

    private static FileDiff.Change getChangeType(String letter) {
        switch (letter) {
            case "M":
                return FileDiff.Change.MODIFICATION;
            case "D":
                return FileDiff.Change.DELETION;
            case "A":
                return FileDiff.Change.ADDITION;
            default:
                if (letter.startsWith("R")) {
                    FileDiff.Change type = FileDiff.Change.RENAME;
                    type.setPercentage(Integer.parseInt(letter.substring(1)));
                    return type;
                } else if (letter.startsWith("C")) {
                    FileDiff.Change type = FileDiff.Change.COPY;
                    type.setPercentage(Integer.parseInt(letter.substring(1)));
                    return type;
                }
        }
        return FileDiff.Change.UNKNOWN;
    }


}
