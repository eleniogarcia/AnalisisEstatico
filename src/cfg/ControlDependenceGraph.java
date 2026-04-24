package cfg;

import java.io.PrintWriter;
import java.util.*;

/**
 * PUNTO 4: Control Dependence Graph (CDG)
 *
 * Algoritmo segun Ferrante et al.:
 * 1. Augmentar el CFG con un nodo START que tiene:
 *    - arista T hacia el nodo ENTRY (primer nodo del programa)
 *    - arista F hacia EXIT
 * 2. Computar postdominadores sobre el grafo augmentado
 * 3. Para cada arista (A->B) en el ACFG donde B NO es ancestro de A en el PDT:
 *    - Subir desde B en el arbol de postdominadores hasta ipdom(A) (sin incluirlo)
 *    - Todos los nodos recorridos son control-dependientes de A
 */
public class ControlDependenceGraph {

    // START -> conjunto de nodos que dependen de el
    public Map<CFGNode, Set<CFGNode>> dependencies = new HashMap<>();
    public CFGNode startNode;

    public void build(List<CFGNode> allNodes,
                      Map<CFGNode, Set<CFGNode>> postDominators,
                      Map<CFGNode, CFGNode> iPostDom,
                      CFGNode entryNode,
                      CFGNode exitNode) {

        // Inicializar mapa para todos los nodos
        for (CFGNode n : allNodes) {
            dependencies.put(n, new LinkedHashSet<>());
        }

        // Crear nodo START artificial
        startNode = new CFGNode("START");
        dependencies.put(startNode, new LinkedHashSet<>());

        // START -> ENTRY (arista T) y START -> EXIT (arista F)
        // Para el CDG: los nodos que no son postdominados por nadie
        // (o sea los que siempre se ejecutan) dependen de START

        // Procesar aristas del CFG augmentado:
        // Las aristas reales del CFG + las dos aristas de START
        List<CFGNode[]> edges = new ArrayList<>();

        // Aristas reales del CFG
        for (CFGNode a : allNodes) {
            for (CFGNode b : a.successors) {
                edges.add(new CFGNode[]{a, b});
            }
        }
        // Aristas artificiales de START
        edges.add(new CFGNode[]{startNode, entryNode}); // T
        edges.add(new CFGNode[]{startNode, exitNode});  // F

        // Para cada arista (A -> B):
        for (CFGNode[] edge : edges) {
            CFGNode a = edge[0];
            CFGNode b = edge[1];

            // Obtener pdom de A (START no tiene pdom calculado, tratarlo como vacio)
            Set<CFGNode> pdomA = postDominators.getOrDefault(a, new HashSet<>());

            // Si B postdomina a A -> skip (no hay dependencia)
            if (pdomA.contains(b)) continue;

            // Subir desde B en el PDT hasta ipdom(A), marcando cada nodo
            // ipdom(START) = null (START no esta en el PDT)
            CFGNode ipdomA = (a == startNode) ? null : iPostDom.get(a);
            CFGNode current = b;

            while (current != null && current != ipdomA) {
                dependencies.get(a).add(current);
                current = iPostDom.get(current);
            }
        }
    }

    public void print(List<CFGNode> allNodes) {
        System.out.println("\n--- Control Dependence Graph (Punto 4) ---");
        System.out.println("Formato: [nodo X] controla -> { nodos dependientes }\n");

        // Imprimir dependencias de START primero
        Set<CFGNode> startDeps = dependencies.get(startNode);
        if (startDeps != null && !startDeps.isEmpty()) {
            System.out.print("  START --> { ");
            for (CFGNode d : startDeps) System.out.print(d.label + "  ");
            System.out.println("}");
        }

        // Luego el resto
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

        // Nodo START
        out.printf("  n%d [label=\"START\", fillcolor=lightgray, shape=ellipse];%n", startNode.id);

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

        // Aristas desde START
        Set<CFGNode> startDeps = dependencies.get(startNode);
        if (startDeps != null) {
            for (CFGNode dep : startDeps) {
                out.printf("  n%d -> n%d [style=dashed, color=gray];%n", startNode.id, dep.id);
            }
        }

        // Aristas del resto
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