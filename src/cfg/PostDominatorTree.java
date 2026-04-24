package cfg;

import java.io.PrintWriter;
import java.util.*;

public class PostDominatorTree {

    public Map<CFGNode, CFGNode> iPostDom = new HashMap<>();

    public void build(List<CFGNode> allNodes, Map<CFGNode, Set<CFGNode>> postDominators, CFGNode exitNode) {
        for (CFGNode n : allNodes) {
            if (n == exitNode) {
                iPostDom.put(n, null);
                continue;
            }

            // Candidatos: todos los postdominadores de n, excepto n mismo
            Set<CFGNode> candidates = new HashSet<>(postDominators.get(n));
            candidates.remove(n);

            if (candidates.isEmpty()) {
                iPostDom.put(n, null);
                continue;
            }

            // El ipdom de n es el candidato c tal que:
            // c NO postdomina a ningun otro candidato
            // Es decir: c NO aparece en pdom(d) para ningun otro candidato d
            // Esto significa que c es el "mas cercano" a n en el camino hacia EXIT
            CFGNode ipdom = null;
            for (CFGNode candidate : candidates) {
                boolean esInmediato = true;
                for (CFGNode other : candidates) {
                    if (other == candidate) continue;
                    // Si candidate aparece en pdom(other), significa que
                    // other es mas cercano a n que candidate -> candidate NO es ipdom
                    if (postDominators.get(other).contains(candidate)) {
                        esInmediato = false;
                        break;
                    }
                }
                if (esInmediato) {
                    ipdom = candidate;
                    break;
                }
            }

            iPostDom.put(n, ipdom);
        }
    }

    public void exportDot(List<CFGNode> allNodes, CFGNode exitNode, PrintWriter out) {
        out.println("digraph PostDomTree {");
        out.println("  rankdir=TB;");
        out.println("  node [shape=box, fontname=\"Helvetica\", style=filled];");
        for (CFGNode n : allNodes) {
            String color = "lightgreen";
            if (n == exitNode)                     color = "lightcoral";
            else if (n.label.startsWith("if"))     color = "lightyellow";
            else if (n.label.startsWith("while"))  color = "lightsalmon";
            else if (n.label.startsWith("return")) color = "lightblue";
            out.printf("  n%d [label=\"%s\", fillcolor=%s];%n",
                    n.id, n.label.replace("\"", "'"), color);
        }
        for (CFGNode n : allNodes) {
            CFGNode parent = iPostDom.get(n);
            if (parent != null) out.printf("  n%d -> n%d;%n", parent.id, n.id);
        }
        out.println("}");
    }

    public void print(List<CFGNode> allNodes) {
        System.out.println("\n--- Arbol de Postdominadores (Punto 3) ---");
        for (CFGNode n : allNodes) {
            CFGNode parent = iPostDom.get(n);
            String parentLabel = (parent != null) ? parent.label : "(raiz)";
            System.out.printf("  ipdom( %-25s ) = %s%n", n.label, parentLabel);
        }
    }
}