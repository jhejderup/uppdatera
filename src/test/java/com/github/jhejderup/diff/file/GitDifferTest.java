package com.github.jhejderup.diff.file;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.File;
import java.nio.file.Path;
import java.util.stream.Collectors;

public class GitDifferTest {

    private Path getFolder(String folder) {
        return new File(getClass().getClassLoader().getResource("diff/" + folder).getFile()).toPath();
    }

    @Test
    public void testGitDiffModification() {
        var oldFolder = getFolder("old");
        var newFolder = getFolder("new");

        var result = GitDiffer.diff(oldFolder, newFolder).collect(Collectors.toList());

        assertEquals(1, result.size());
        assertEquals(FileDiff.Change.MODIFICATION, result.get(0).type);

        assertTrue(result.get(0).srcFile.get().endsWith("old/test.txt"));
        assertTrue(result.get(0).dstFile.get().endsWith("new/test.txt"));
    }

    @Test
    public void testGitDiffDeletion() {
        var oldFolder = getFolder("old2");
        var newFolder = getFolder("new2");

        var result = GitDiffer.diff(oldFolder, newFolder).collect(Collectors.toList());

        assertEquals(1, result.size());
        assertEquals(FileDiff.Change.DELETION, result.get(0).type);

        assertTrue(result.get(0).srcFile.get().endsWith("old2/test.txt"));
        assertTrue(result.get(0).dstFile.isEmpty());
    }

    @Test
    public void testGitDiffAddition() {
        var oldFolder = getFolder("new2");
        var newFolder = getFolder("old2");

        var result = GitDiffer.diff(oldFolder, newFolder).collect(Collectors.toList());

        assertEquals(1, result.size());
        assertEquals(FileDiff.Change.ADDITION, result.get(0).type);

        assertTrue(result.get(0).srcFile.isEmpty());
        assertTrue(result.get(0).dstFile.get().endsWith("old2/test.txt"));
    }

    @Test
    public void testGitDiffRename() {
        var oldFolder = getFolder("old3");
        var newFolder = getFolder("new3");

        var result = GitDiffer.diff(oldFolder, newFolder).collect(Collectors.toList());

        assertEquals(1, result.size());
        assertEquals(FileDiff.Change.RENAME, result.get(0).type);

        assertTrue(result.get(0).srcFile.get().endsWith("old3/test.txt"));
        assertTrue(result.get(0).dstFile.get().endsWith("new3/test2.txt"));
    }

}
