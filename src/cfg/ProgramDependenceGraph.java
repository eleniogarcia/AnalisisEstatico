package cfg;

import java.io.PrintWriter;
import java.util.*;

/**
 * Program Dependence Graph (PDG).
 *
 * Union del CDG y el DDG:
 * - Aristas de control (del CDG): X controla a Y
 * - Aristas de datos (del DDG):   X define una variable que usa Y
 *
 * Se usa como base para el Program Slicing.
 */
public class ProgramDependenceGraph {

    // Para cada nodo: conjunto de nodos de los que DEPENDE (aristas hacia atras)
    // predecessors en el PDG = quien me afecta a mi
    public Map<CFGNode, Set<CFGNode>> predecessors = new HashMap<>();

    // Para cada nodo: conjunto de nodos a los que AFECTA (aristas hacia adelante)
    public Map<CFGNode, Set<CFGNode>> successors = new HashMap<>();

    public void build(List<CFGNode> allNodes,
                      ControlDependenceGraph cdg,
                      DataDependenceGraph ddg) {

        // Inicializar mapas
        for (CFGNode n : allNodes) {
            predecessors.put(n, new LinkedHashSet<>());
            successors.put(n,   new LinkedHashSet<>());
        }
        // Agregar nodo START del CDG
        if (cdg.startNode != null) {
            predecessors.put(cdg.startNode, new LinkedHashSet<>());
            successors.put(cdg.startNode,   new LinkedHashSet<>());
        }

        // --- Aristas del CDG: X -> Y significa "Y depende de control de X" ---
        // En el PDG: arista X -> Y (X es predecesor de Y)
        for (CFGNode x : cdg.dependencies.keySet()) {
            Set<CFGNode> deps = cdg.dependencies.get(x);
            if (deps == null) continue;
            for (CFGNode y : deps) {
                // asegurar que y este en el mapa
                predecessors.computeIfAbsent(y, k -> new LinkedHashSet<>()).add(x);
                successors.computeIfAbsent(x, k -> new LinkedHashSet<>()).add(y);
            }
        }

        // --- Aristas del DDG: par (D, U) significa "U depende de datos de D" ---
        // En el PDG: arista D -> U (D es predecesor de U)
        for (DataDependenceGraph.DUPair pair : ddg.duPairs) {
            predecessors.computeIfAbsent(pair.use, k -> new LinkedHashSet<>()).add(pair.def);
            successors.computeIfAbsent(pair.def,   k -> new LinkedHashSet<>()).add(pair.use);
        }
    }

    public void exportDot(List<CFGNode> allNodes,
                          ControlDependenceGraph cdg,
                          DataDependenceGraph ddg,
                          PrintWriter out) {
        out.println("digraph PDG {");
        out.println("  rankdir=TB;");
        out.println("  node [shape=box, fontname=\"Helvetica\", style=filled];");

        // Nodo START
        if (cdg.startNode != null) {
            out.printf("  n%d [label=\"START\", fillcolor=lightgray, shape=ellipse];%n",
                    cdg.startNode.id);
        }

        // Demas nodos
        for (CFGNode n : allNodes) {
            String color = "lightblue";
            if (n.label.startsWith("if"))      color = "lightyellow";
            else if (n.label.startsWith("while"))  color = "lightsalmon";
            else if (n.label.startsWith("return")) color = "lightcoral";
            else if (n.label.equals("EXIT"))        color = "gray";
            out.printf("  n%d [label=\"%s\", fillcolor=%s];%n",
                    n.id, n.label.replace("\"", "'"), color);
        }

        out.println();

        // Aristas de control (CDG) — gris punteado
        for (CFGNode x : cdg.dependencies.keySet()) {
            for (CFGNode y : cdg.dependencies.get(x)) {
                out.printf("  n%d -> n%d [style=dashed, color=gray, label=\"ctrl\"];%n",
                        x.id, y.id);
            }
        }

        // Aristas de datos (DDG) — verde solido
        for (DataDependenceGraph.DUPair p : ddg.duPairs) {
            out.printf("  n%d -> n%d [color=darkgreen, label=\"%s\"];%n",
                    p.def.id, p.use.id, p.variable);
        }

        out.println("}");
    }
}