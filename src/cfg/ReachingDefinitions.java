package cfg;

import ast.AssignStmt;
import java.util.*;

/**
 * Reaching Definitions — Análisis de Data Flow (forward, may).
 *
 * Para cada nodo n calcula:
 *   GEN(n)  = definición que produce el nodo (solo AssignStmt genera definición)
 *   KILL(n) = todas las otras definiciones de la misma variable en el programa
 *   IN(n)   = union de OUT de todos los predecesores
 *   OUT(n)  = GEN(n) U (IN(n) - KILL(n))
 *
 * Una "definición" se representa como el par (CFGNode, String variable).
 */
public class ReachingDefinitions {

    // Para cada nodo: su definición generada. Null si no genera ninguna.
    public Map<CFGNode, Definition> gen  = new HashMap<>();

    // Para cada nodo: el conjunto de definiciones que mata
    public Map<CFGNode, Set<Definition>> kill = new HashMap<>();

    // Resultados del análisis iterativo
    public Map<CFGNode, Set<Definition>> in  = new HashMap<>();
    public Map<CFGNode, Set<Definition>> out = new HashMap<>();

    // Todas las definiciones del programa
    private Set<Definition> allDefs = new LinkedHashSet<>();

    /**
     * Representa una definición: el nodo donde se define y la variable definida.
     */
    public static class Definition {
        public CFGNode node;
        public String  variable;

        public Definition(CFGNode node, String variable) {
            this.node     = node;
            this.variable = variable;
        }

        @Override
        public String toString() {
            return node.id + ":" + variable;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Definition)) return false;
            Definition d = (Definition) o;
            return node == d.node && variable.equals(d.variable);
        }

        @Override
        public int hashCode() {
            return Objects.hash(System.identityHashCode(node), variable);
        }
    }

    public void compute(List<CFGNode> allNodes) {

        // --- Paso 1: Calcular GEN de cada nodo ---
        for (CFGNode n : allNodes) {
            if (n.astNode instanceof AssignStmt) {
                AssignStmt a = (AssignStmt) n.astNode;
                Definition def = new Definition(n, a.id);
                gen.put(n, def);
                allDefs.add(def);
            } else {
                gen.put(n, null);
            }
        }

        // --- Paso 2: Calcular KILL de cada nodo ---
        // Un nodo que define variable v mata todas las otras definiciones de v
        for (CFGNode n : allNodes) {
            Set<Definition> killSet = new LinkedHashSet<>();
            Definition myDef = gen.get(n);
            if (myDef != null) {
                for (Definition d : allDefs) {
                    if (d.variable.equals(myDef.variable) && d.node != n) {
                        killSet.add(d);
                    }
                }
            }
            kill.put(n, killSet);
        }

        // --- Paso 3: Inicializar IN y OUT ---
        for (CFGNode n : allNodes) {
            in.put(n,  new LinkedHashSet<>());
            // OUT se inicializa con GEN
            Set<Definition> outSet = new LinkedHashSet<>();
            if (gen.get(n) != null) outSet.add(gen.get(n));
            out.put(n, outSet);
        }

        // --- Paso 4: Iterar hasta punto fijo ---
        boolean changed = true;
        while (changed) {
            changed = false;
            for (CFGNode n : allNodes) {

                // IN(n) = union de OUT de todos los predecesores
                Set<Definition> inSet = new LinkedHashSet<>();
                for (CFGNode pred : n.predecessors) {
                    inSet.addAll(out.get(pred));
                }
                in.put(n, inSet);

                // OUT(n) = GEN(n) U (IN(n) - KILL(n))
                Set<Definition> newOut = new LinkedHashSet<>();
                if (gen.get(n) != null) newOut.add(gen.get(n));
                for (Definition d : inSet) {
                    if (!kill.get(n).contains(d)) newOut.add(d);
                }

                if (!newOut.equals(out.get(n))) {
                    out.put(n, newOut);
                    changed = true;
                }
            }
        }
    }

    public void print(List<CFGNode> allNodes) {
        System.out.println("\n--- Reaching Definitions ---");
        System.out.printf("  %-28s %-30s %-30s%n", "Nodo", "IN", "OUT");
        System.out.println("  " + "-".repeat(88));
        for (CFGNode n : allNodes) {
            System.out.printf("  %-28s %-30s %-30s%n",
                    n.label,
                    setToString(in.get(n)),
                    setToString(out.get(n)));
        }
    }

    private String setToString(Set<Definition> set) {
        if (set == null || set.isEmpty()) return "{}";
        StringBuilder sb = new StringBuilder("{");
        for (Definition d : set) sb.append(d).append(" ");
        sb.append("}");
        return sb.toString();
    }
}