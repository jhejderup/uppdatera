package com.github.jhejderup.data;

import java.io.Serializable;
import java.nio.file.Path;
import java.util.Optional;


public class FileDiff implements Serializable {
    public final Optional<Path> source;
    public final Optional<Path> destination;
    public final Change type;

    public FileDiff(Optional<Path> source, Optional<Path> destination, Change type) {
        this.source = source;
        this.destination = destination;
        this.type = type;
    }

    @Override
    public String toString() {
        return "FileDiff(" + type.name() + "," + source.toString() + "," + destination.toString() + ")";
    }

    public enum Change {
        MODIFICATION(0),
        ADDITION(0),
        DELETION(0),
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

    }
}

