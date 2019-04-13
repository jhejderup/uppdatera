package com.github.jhejderup.data.diff;

import java.io.Serializable;
import java.nio.file.Path;
import java.util.Optional;


public final class FileDiff implements Serializable {
    public final Optional<Path> srcFile;
    public final Optional<Path> dstFile;
    public final Change type;

    public FileDiff(Optional<Path> srcFile, Optional<Path> dstFile, Change type) {
        this.srcFile = srcFile;
        this.dstFile = dstFile;
        this.type = type;
    }

    public static Change getChangeType(String statusCode) {

        if (statusCode.startsWith("M")) {
            return Change.MODIFICATION;
        } else if (statusCode.startsWith("D")) {
            return Change.DELETION;
        } else if (statusCode.startsWith("A")) {
            return Change.ADDITION;
        } else if (statusCode.startsWith("R")) {
            Change type = FileDiff.Change.RENAME;
            type.setPercentage(Integer.parseInt(statusCode.substring(1)));
            return type;
        } else if (statusCode.startsWith("C")) {
            Change type = FileDiff.Change.COPY;
            type.setPercentage(Integer.parseInt(statusCode.substring(1)));
            return type;
        } else {
            return Change.UNKNOWN;
        }
    }

    @Override
    public String toString() {
        return "FileDiff(" + type + "," + srcFile.toString() + "," + dstFile.toString() + ")";
    }

    public enum Change {
        MODIFICATION(100),
        ADDITION(100),
        DELETION(100),
        COPY(0),
        RENAME(0),
        UNKNOWN(0);
        private int percentage;

        Change(int percentage) {
            this.percentage = percentage;
        }

        public void setPercentage(int percentage) {
            this.percentage = percentage;
        }

        @Override
        public String toString() {
            return this.name() + "(" + this.percentage + ")";
        }
    }
}

