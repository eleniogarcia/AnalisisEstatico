package cfg;

import java.io.PrintWriter;
import java.util.*;

public class ControlDependenceGraph {

    public Map<CFGNode, Set<CFGNode>> dependencies = new HashMap<>();

    public void build(List<CFGNode> allNodes,
                      Map<CFGNode, Set<CFGNode>> postDominators,
                      Map<CFGNode, CFGNode> iPostDom,
                      CFGNode exitNode) {

        for (CFGNode n : allNodes) {
            dependencies.put(n, new LinkedHashSet<>());
        }

        for (CFGNode a : allNodes) {
            for (CFGNode b : a.successors) {
                if (postDominators.get(a).contains(b)) continue;

                CFGNode ipdomA = iPostDom.get(a);
                CFGNode current = b;
                while (current != null && current != ipdomA) {
                    dependencies.get(a).add(current);
                    current = iPostDom.get(current);
                }
            }
        }
    }

    public void print(List<CFGNode> allNodes) {
        System.out.println("\n--- Control Dependence Graph (Punto 4) ---");
        for (CFGNode n : allNodes) {
            Set<CFGNode> deps = dependencies.get(n);
            if (deps != null && !deps.isEmpty()) {
                System.out.print("  " + n.label + " --> { ");
                for (CFGNode d : deps) System.out.print(d.label + "  ");
                System.out.println("}");
            }
        }
    }

    public void exportDot(List<CFGNode> allNodes, PrintWriter out) {
        out.println("digraph CDG {");
        out.println("  rankdir=TB;");
        out.println("  node [shape=box, fontname=\"Helvetica\", style=filled];");
        for (CFGNode n : allNodes) {
            String color = "lightblue";
            if (n.label.startsWith("if"))      color = "lightyellow";
            else if (n.label.startsWith("while"))   color = "lightsalmon";
            else if (n.label.startsWith("return"))  color = "lightcoral";
            else if (n.label.equals("EXIT"))         color = "gray";
            out.printf("  n%d [label=\"%s\", fillcolor=%s];%n",
                    n.id, n.label.replace("\"", "'"), color);
        }
        for (CFGNode n : allNodes) {
            Set<CFGNode> deps = dependencies.get(n);
            if (deps == null) continue;
            for (CFGNode dep : deps) {
                out.printf("  n%d -> n%d [style=dashed, color=blue];%n", n.id, dep.id);
            }
        }
        out.println("}");
    }
}