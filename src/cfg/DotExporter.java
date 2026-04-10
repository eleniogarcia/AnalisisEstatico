package cfg;

import java.io.PrintWriter;
import java.util.*;

public class DotExporter {

    public void export(CFGNode start, PrintWriter out) {
        out.println("digraph CFG {");
        out.println("  node [shape=box, fontname=\"Helvetica\", style=filled, fillcolor=lightblue];");

        Set<Integer> visited = new HashSet<>();
        Deque<CFGNode> stack = new ArrayDeque<>();
        stack.push(start);

        while (!stack.isEmpty()) {
            CFGNode n = stack.pop();
            if (visited.contains(n.id)) continue;
            visited.add(n.id);

            // Nodos especiales con color distinto
            String color = "lightblue";
            if (n.label.startsWith("if"))    color = "lightyellow";
            if (n.label.startsWith("while")) color = "lightsalmon";
            if (n.label.startsWith("return"))color = "lightcoral";
            if (n.label.equals("merge"))     color = "lightgray";

            out.printf("  n%d [label=\"%s\", fillcolor=%s];%n",
                    n.id, n.label.replace("\"", "'"), color);

            for (CFGNode succ : n.successors) {
                out.printf("  n%d -> n%d;%n", n.id, succ.id);
                stack.push(succ);
            }
        }

        out.println("}");
    }
}