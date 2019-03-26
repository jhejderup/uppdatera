package com.github.jhejderup.diff;

import com.github.gumtreediff.tree.ITree;
import com.github.gumtreediff.tree.TreeContext;

public class ChangeClassifier {


    static ITree parentNode;
    static ITree currentNode;

    public static ITree getParentNode()
    {
        return parentNode;
    }

    public static ITree getCurrentNode()
    {
        return currentNode;
    }


    public static boolean isMethodChange(TreeContext tsrc, ITree node) {
        boolean flag = false;
        ITree parentnode = node.getParent();
        ITree currentnode = node;

        while (parentnode != null) {
            String currentnodetype = tsrc.getTypeLabel(currentnode.getType());
            String parentnodetype = tsrc.getTypeLabel(parentnode.getType());

            if (!currentnodetype.equals("Block") && !currentnodetype.equals("Javadoc") && parentnodetype.equals("MethodDeclaration")) {
                currentNode = currentnode;
                parentNode = parentnode;
                flag = true;
                break;
            }

            currentnode = parentnode;
            parentnode = currentnode.getParent();
        }

        return flag;
    }


}