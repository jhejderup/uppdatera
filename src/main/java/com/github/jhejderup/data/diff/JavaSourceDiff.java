package com.github.jhejderup.data.diff;

import gumtree.spoon.diff.operations.Operation;
import spoon.reflect.declaration.CtExecutable;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class JavaSourceDiff implements Serializable {

    public final FileDiff fileDiff;
    public final Optional<Map<CtExecutable, List<Operation>>> methodDiffs;


    public JavaSourceDiff(FileDiff fileDiff, Optional<Map<CtExecutable, List<Operation>>> methodDiffs){
        this.fileDiff = fileDiff;
        this.methodDiffs = methodDiffs;
    }

}
