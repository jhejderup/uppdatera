package com.github.jhejderup.diff;

import gumtree.spoon.AstComparator;
import gumtree.spoon.diff.Diff;
import gumtree.spoon.diff.operations.Operation;
import spoon.reflect.declaration.CtConstructor;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtExecutable;
import spoon.reflect.declaration.CtMethod;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class JavaSourceDiff {

    public static Map<String, List<Operation>> editMethodScript(String left, String right) {

        AstComparator diff = new AstComparator();
        Diff editScript = diff.compare(left, right);
        return editScript
                .getRootOperations()
                .stream()
                .filter(op -> isChangeInMethod(op.getSrcNode()))
                .collect(Collectors.toMap(
                        op -> ((CtExecutable) getBelongingMethod(op.getSrcNode()).get()).getSignature(),
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
        return getBelongingMethod(node).isPresent();
    }

    private static Optional<CtElement> getBelongingMethod(CtElement node) {
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
}
