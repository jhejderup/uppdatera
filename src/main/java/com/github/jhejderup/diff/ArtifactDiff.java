package com.github.jhejderup.diff;

import com.github.jhejderup.data.FileDiff;
import com.github.jhejderup.data.MavenCoordinate;
import com.github.jhejderup.data.JavaSourceDiff;
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

    public static Stream<JavaSourceDiff> diff(MavenCoordinate coordLeft, MavenCoordinate coordRight)
            throws IOException, TimeoutException, InterruptedException {
        Path leftFolder = fetchAndExtractJARSource(coordLeft);
        Path rightFolder = fetchAndExtractJARSource(coordRight);

        return diffFiles(leftFolder, rightFolder)
                .filter(ArtifactDiff::isImpactKind)
                .filter(ArtifactDiff::isJavaFile)
                .filter(ArtifactDiff::isNotTestFile)
                .map(ArtifactDiff::diffJavaMethods);
    }
    ///
    /// Download and unzip
    ///
    private static Path fetchAndExtractJARSource(MavenCoordinate coordinate)
            throws IOException {
        File jarFile = Maven.resolver()
                .resolve(coordinate.groupId
                        + ":"
                        + coordinate.artifactId
                        + ":java-source:sources:"
                        + coordinate.version)
                .withoutTransitivity()
                .asSingleFile();
        return unzip(jarFile);
    }

    private static Path unzip(final File zipFilePath)
            throws IOException {

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

    private static void unzipFiles(final ZipInputStream zipInputStream, final Path unzipFilePath)
            throws IOException {

        try (BufferedOutputStream bos = new BufferedOutputStream(
                new FileOutputStream(unzipFilePath.toAbsolutePath().toString()))) {
            byte[] bytesIn = new byte[1024];
            int read = 0;
            while ((read = zipInputStream.read(bytesIn)) != -1) {
                bos.write(bytesIn, 0, read);
            }
        }

    }
    ///
    /// File level diffing using git diff
    ///
    private static String executeGitDiff(Path leftFolder, Path rightFolder)
            throws InterruptedException, TimeoutException, IOException {
        return new ProcessExecutor()
                .command("git", "diff", "--no-index", "--name-status", leftFolder.toString(), rightFolder.toString())
                .readOutput(true).execute()
                .outputString();
    }

    private static boolean isImpactKind(FileDiff diff) {
        return diff.type == FileDiff.Change.DELETION ||
                diff.type == FileDiff.Change.RENAME ||
                diff.type == FileDiff.Change.MODIFICATION;
    }

    private static boolean isJavaFile(FileDiff diff) {
        Optional<Path> src = diff.srcFile;
        return src.map(path -> path.toString().endsWith(".java")).orElse(false);
    }

    private static boolean isNotTestFile(FileDiff diff) {
        Optional<Path> src = diff.srcFile;
       return src.map(path -> !path.toString().endsWith("/src/test/")).orElse(true);
    }

    private static Stream<FileDiff> diffFiles(Path leftFolder, Path rightFolder)
            throws InterruptedException, TimeoutException, IOException {
        String rawOutput = executeGitDiff(leftFolder, rightFolder);
        return Arrays.stream(rawOutput.split("\n"))
                .map(line -> line.split("\t"))
                .map(arr -> {
                    String srcFile = arr[1];
                    FileDiff.Change mode = FileDiff.getChangeType(arr[0]);
                    if (arr.length == 3) {
                        String dstFile = arr[2];
                        return new FileDiff(
                                Optional.of(Paths.get(srcFile)),
                                Optional.of(Paths.get(dstFile)),
                                mode);
                    } else {
                        switch (mode) {
                            case MODIFICATION:
                                String filename = srcFile.replace(leftFolder.toString(), "");
                                return new FileDiff(
                                        Optional.of(Paths.get(srcFile)),
                                        Optional.of(Paths.get(rightFolder + filename)),
                                        mode);
                            case DELETION:
                                return new FileDiff(
                                        Optional.of(Paths.get(srcFile)),
                                        Optional.empty(),
                                        mode);
                            case ADDITION:
                                return new FileDiff(
                                        Optional.empty(),
                                        Optional.of(Paths.get(srcFile)),
                                        mode);
                            default:
                                return null;
                        }
                    }
                })
                .filter(Objects::nonNull); //we currently skip unknown (null diffs)
    }
    ///
    /// AST level diffing using GumTree/Spoon
    ///
    private static boolean isFileRemoval(FileDiff diff) {
        return diff.type == FileDiff.Change.DELETION || diff.type == FileDiff.Change.RENAME;
    }

    private static boolean isMethod(CtElement element) {
        return element instanceof CtMethod;
    }

    private static boolean isConstructor(CtElement element) {
        return element instanceof CtConstructor;
    }

    private static boolean isMethodChange(Operation op) {
        return getTopLevelMethod(op).isPresent();
    }


    private static Optional<CtElement> getTopLevelMethod(Operation op) {
        CtElement parent = op.getSrcNode();
        Stack<CtElement> stack = new Stack<>();
        try {
            //1. keep traversing parents in the tree
            //   until we see the sky
            //   and push elements to the stack
            while (parent.getParent() != null) {
                stack.push(parent);
                parent = parent.getParent();
            }
            //2. pop until we find our top-level method
            if (stack.size() > 0) {
                CtElement method = stack.pop();
                while (stack.size() > 0) {
                    if (isMethod(method) || isConstructor(method)) {
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

    private static Stream<Operation> diffJavaSourceFiles(FileDiff fileDiff)
            throws Exception {
        assert (fileDiff.type != FileDiff.Change.ADDITION);
        assert (fileDiff.type != FileDiff.Change.COPY);

        AstComparator diff = new AstComparator();
        Optional<Path> srcFile = fileDiff.srcFile;
        Optional<Path> dstFile = fileDiff.dstFile;

        //file removal -> allOperations due to comp. with null
        List<Operation> editScript = isFileRemoval(fileDiff) ?
                diff.compare(diff.getCtType(srcFile.get().toFile()), null).getAllOperations() :
                diff.compare(srcFile.get().toFile(), dstFile.get().toFile()).getRootOperations();

        return editScript
                .stream();
    }

    private static Map<CtExecutable, List<Operation>> diffJavaSourceMethods(FileDiff fileDiff)
            throws Exception {

        return diffJavaSourceFiles(fileDiff)
                .filter(ArtifactDiff::isMethodChange) //only changes in a method/constructor
                .collect(Collectors.groupingBy(op -> ((CtExecutable) getTopLevelMethod(op).get())));
    }

    private static JavaSourceDiff diffJavaMethods(FileDiff fileDiff) {
        try {
            Map<CtExecutable, List<Operation>> methodDiffs = diffJavaSourceMethods(fileDiff);
            return new JavaSourceDiff(fileDiff, Optional.of(methodDiffs));
        } catch (Exception e) {
            e.printStackTrace();
            return new JavaSourceDiff(fileDiff, Optional.empty());
        }
    }
}
