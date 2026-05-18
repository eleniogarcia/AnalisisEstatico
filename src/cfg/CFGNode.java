package cfg;

import ast.Node;
import java.util.ArrayList;
import java.util.List;

public class CFGNode {
    public String label;
    public Node astNode;
    public List<CFGNode> successors   = new ArrayList<>();
    public List<CFGNode> predecessors = new ArrayList<>();
    private static int counter = 0;

    public static void resetCounter() { counter = 0; }
    public int id;

    public CFGNode(String label, Node astNode) {
        this.label   = label;
        this.astNode = astNode;
        this.id      = counter++;
    }

    public CFGNode(String label) {
        this(label, null);
    }

    public void addEdge(CFGNode target) {
        successors.add(target);
        target.predecessors.add(this);
    }

    public boolean hasAstNode() {
        return astNode != null;
    }
}