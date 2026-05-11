package cfg;

import ast.AssignStmt;
import ast.ReturnStmt;
import ast.IfStmt;
import ast.WhileStmt;

import java.io.PrintWriter;
import java.util.*;

/**
 * Data Dependence Graph (DDG).
 *
 * Construido a partir de los pares Definición-Uso (DU Pairs).
 * Un par (D, U) existe si:
 *   - D define una variable v
 *   - U usa esa variable v
 *   - D está en IN(U) (la definición llega a U sin ser matada)
 *
 * Cada par DU genera una arista D -> U en el DDG.
 */
public class DataDependenceGraph {

    // Par definición-uso
    public static class DUPair {
        public CFGNode def;   // nodo que define la variable
        public CFGNode use;   // nodo que usa la variable
        public String  variable;

        public DUPair(CFGNode def, CFGNode use, String variable) {
            this.def      = def;
            this.use      = use;
            this.variable = variable;
        }

        @Override
        public String toString() {
            return "[" + def.id + ":" + variable + " → " + use.id + ":" + variable + "]";
        }
    }

    public List<DUPair> duPairs = new ArrayList<>();

    public void build(List<CFGNode> allNodes, ReachingDefinitions rd) {

        for (CFGNode useNode : allNodes) {
            // Obtener las variables usadas en este nodo
            Set<String> usedVars = getUsedVars(useNode);
            if (usedVars.isEmpty()) continue;

            // Para cada variable usada, buscar si hay una definición en IN(useNode)
            Set<ReachingDefinitions.Definition> inSet = rd.in.get(useNode);
            if (inSet == null) continue;

            for (String var : usedVars) {
                for (ReachingDefinitions.Definition def : inSet) {
                    if (def.variable.equals(var)) {
                        duPairs.add(new DUPair(def.node, useNode, var));
                    }
                }
            }
        }
    }

    /**
     * Extrae las variables USADAS (no definidas) en un nodo CFG.
     * Analiza el objeto AST del nodo para obtener los operandos.
     */
    private Set<String> getUsedVars(CFGNode node) {
        Set<String> used = new LinkedHashSet<>();
        if (node.astNode == null) return used;

        if (node.astNode instanceof AssignStmt) {
            // x := a + b  →  usa: a, b (todo lo que está a la derecha del :=)
            AssignStmt a = (AssignStmt) node.astNode;
            extractVarsFromExpr(a.expr, used);

        } else if (node.astNode instanceof ReturnStmt) {
            // return expr  →  usa las vars de expr
            ReturnStmt r = (ReturnStmt) node.astNode;
            extractVarsFromExpr(r.expr, used);

        } else if (node.astNode instanceof IfStmt) {
            // if (cond)  →  usa las vars de la condición
            IfStmt i = (IfStmt) node.astNode;
            extractVarsFromExpr(i.condition, used);

        } else if (node.astNode instanceof WhileStmt) {
            // while (cond)  →  usa las vars de la condición
            WhileStmt w = (WhileStmt) node.astNode;
            extractVarsFromExpr(w.condition, used);
        }

        return used;
    }

    /**
     * Extrae identificadores (variables) de una expresión como "a + b" o "x".
     * Ignora los números.
     */
    private void extractVarsFromExpr(String expr, Set<String> result) {
        if (expr == null) return;
        // Separar por el operador +
        String[] parts = expr.split("\\+");
        for (String part : parts) {
            String token = part.trim();
            if (!token.isEmpty() && !token.matches("[0-9]+")) {
                result.add(token);
            }
        }
    }

    public void print(List<CFGNode> allNodes) {
        System.out.println("\n--- Data Dependence Graph - Pares DU (Definición-Uso) ---");
        if (duPairs.isEmpty()) {
            System.out.println("  (No se encontraron pares definición-uso)");
            return;
        }

        // Agrupar por nodo de uso para mejor legibilidad
        Map<CFGNode, List<DUPair>> byUse = new LinkedHashMap<>();
        for (CFGNode n : allNodes) byUse.put(n, new ArrayList<>());
        for (DUPair p : duPairs) byUse.get(p.use).add(p);

        System.out.printf("  %-28s  %s%n", "Nodo USO", "Pares (def_nodo:variable → uso_nodo:variable)");
        System.out.println("  " + "-".repeat(70));
        for (CFGNode n : allNodes) {
            List<DUPair> pairs = byUse.get(n);
            if (!pairs.isEmpty()) {
                System.out.printf("  %-28s  ", n.label);
                for (DUPair p : pairs) System.out.print(p + "  ");
                System.out.println();
            }
        }
    }

    public void exportDot(List<CFGNode> allNodes, PrintWriter out) {
        out.println("digraph DDG {");
        out.println("  rankdir=TB;");
        out.println("  node [shape=box, fontname=\"Helvetica\", style=filled];");

        // Declarar nodos
        for (CFGNode n : allNodes) {
            String color = "lightyellow";
            if (n.label.startsWith("if"))      color = "lightyellow";
            else if (n.label.startsWith("while"))  color = "lightsalmon";
            else if (n.label.startsWith("return")) color = "lightcoral";
            else if (n.label.equals("EXIT"))        color = "gray";
            else                                    color = "lightblue";
            out.printf("  n%d [label=\"%s\", fillcolor=%s];%n",
                    n.id, n.label.replace("\"", "'"), color);
        }

        out.println();

        // Una arista por par DU, etiquetada con la variable
        for (DUPair p : duPairs) {
            out.printf("  n%d -> n%d [label=\"%s\", color=darkgreen, fontcolor=darkgreen];%n",
                    p.def.id, p.use.id, p.variable);
        }

        out.println("}");
    }
}