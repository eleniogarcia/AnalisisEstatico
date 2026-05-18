package cfg;

import java.io.PrintWriter;
import java.util.*;

/**
 * Program Slicer — Backward Slicing sobre el PDG.
 *
 * Dado un criterio de slicing (un nodo del CFG), recorre el PDG
 * hacia atras (siguiendo las aristas inversas) y recolecta todos
 * los nodos que afectan al nodo seleccionado, ya sea por:
 *   - Dependencia de control (aristas del CDG)
 *   - Dependencia de datos   (aristas del DDG)
 *
 * Todos los nodos alcanzados forman el SLICE del programa.
 */
public class ProgramSlicer {

    // Resultado: conjunto de nodos que forman el slice
    public Set<CFGNode> slice = new LinkedHashSet<>();

    // Nodo criterio seleccionado
    public CFGNode criterion;

    /**
     * Calcula el backward slice a partir del nodo criterio.
     *
     * @param criterion el nodo desde el cual calcular el slice
     * @param pdg       el Program Dependence Graph
     */
    public void compute(CFGNode criterion, ProgramDependenceGraph pdg) {
        this.criterion = criterion;
        slice.clear();

        // BFS hacia atras en el PDG
        Queue<CFGNode> queue   = new LinkedList<>();
        Set<CFGNode>   visited = new LinkedHashSet<>();

        queue.add(criterion);
        visited.add(criterion);

        while (!queue.isEmpty()) {
            CFGNode current = queue.poll();
            slice.add(current);

            // Obtener predecesores en el PDG (quienes afectan a current)
            Set<CFGNode> preds = pdg.predecessors.get(current);
            if (preds == null) continue;

            for (CFGNode pred : preds) {
                if (visited.add(pred)) {
                    queue.add(pred);
                }
            }
        }
    }

    public void print() {
        System.out.println("\n--- Backward Slice ---");
        System.out.println("Criterio: " + criterion.label);
        System.out.println("Instrucciones en el slice:");
        for (CFGNode n : slice) {
            if (!n.label.equals("EXIT") && !n.label.equals("START")) {
                System.out.println("  → " + n.label);
            }
        }
    }

    /**
     * Exporta el slice como un grafo DOT, resaltando los nodos
     * que pertenecen al slice sobre el CFG completo.
     */
    public void exportDot(List<CFGNode> allNodes, CFGNode exitNode, PrintWriter out) {
        out.println("digraph Slice {");
        out.println("  rankdir=TB;");
        out.println("  node [shape=box, fontname=\"Helvetica\", style=filled];");

        // Declarar todos los nodos: resaltados si estan en el slice
        for (CFGNode n : allNodes) {
            boolean inSlice = slice.contains(n);
            String color;
            if (n == criterion)                    color = "gold";         // criterio en dorado
            else if (inSlice && n.label.startsWith("if"))    color = "yellow";
            else if (inSlice && n.label.startsWith("while")) color = "orange";
            else if (inSlice && n.label.startsWith("return"))color = "tomato";
            else if (inSlice)                      color = "lightgreen";   // en el slice
            else                                   color = "lightgray";    // fuera del slice

            String border = inSlice ? ", penwidth=3" : "";
            out.printf("  n%d [label=\"%s\", fillcolor=%s%s];%n",
                    n.id, n.label.replace("\"", "'"), color, border);
        }

        out.println();

        // Aristas del CFG original
        Set<String> drawn = new HashSet<>();
        for (CFGNode n : allNodes) {
            for (CFGNode succ : n.successors) {
                String key = n.id + "->" + succ.id;
                if (drawn.add(key)) {
                    // Resaltar aristas entre nodos del slice
                    boolean highlighted = slice.contains(n) && slice.contains(succ);
                    String style = highlighted ? "color=green, penwidth=2" : "color=lightgray";
                    out.printf("  n%d -> n%d [%s];%n", n.id, succ.id, style);
                }
            }
        }

        out.println("}");
    }
}