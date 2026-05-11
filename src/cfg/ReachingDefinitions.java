package cfg;

import ast.AssignStmt;
import java.io.PrintWriter;
import java.util.*;

public class ReachingDefinitions {

    public Map<CFGNode, Definition> gen  = new HashMap<>();
    public Map<CFGNode, Set<Definition>> kill = new HashMap<>();
    public Map<CFGNode, Set<Definition>> in  = new HashMap<>();
    public Map<CFGNode, Set<Definition>> out = new HashMap<>();

    private Set<Definition> allDefs = new LinkedHashSet<>();

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

        // Paso 1: GEN
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

        // Paso 2: KILL
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

        // Paso 3: Inicializar IN y OUT
        for (CFGNode n : allNodes) {
            in.put(n, new LinkedHashSet<>());
            Set<Definition> outSet = new LinkedHashSet<>();
            if (gen.get(n) != null) outSet.add(gen.get(n));
            out.put(n, outSet);
        }

        // Paso 4: Iterar hasta punto fijo
        boolean changed = true;
        while (changed) {
            changed = false;
            for (CFGNode n : allNodes) {
                Set<Definition> inSet = new LinkedHashSet<>();
                for (CFGNode pred : n.predecessors) {
                    inSet.addAll(out.get(pred));
                }
                in.put(n, inSet);

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

    public void exportDot(List<CFGNode> allNodes, PrintWriter out) {
        out.println("digraph ReachingDefinitions {");
        out.println("  rankdir=TB;");
        out.println("  node [shape=box, fontname=\"Helvetica\", style=filled, fillcolor=lightyellow];");

        for (CFGNode n : allNodes) {
            Definition myGen    = gen.get(n);
            Set<Definition> myKill = kill.get(n);
            Set<Definition> myIn   = in.get(n);
            Set<Definition> myOut  = this.out.get(n);

            String genStr  = (myGen != null) ? "{" + myGen + "}" : "{}";
            String killStr = (myKill != null && !myKill.isEmpty()) ? setToString(myKill) : "{}";
            String inStr   = (myIn   != null && !myIn.isEmpty())   ? setToString(myIn)   : "{}";
            String outStr  = (myOut  != null && !myOut.isEmpty())  ? setToString(myOut)  : "{}";

            String label = n.label + "\\n"
                    + "GEN="  + genStr  + "\\n"
                    + "KILL=" + killStr + "\\n"
                    + "IN="   + inStr   + "\\n"
                    + "OUT="  + outStr;

            String color = "lightyellow";
            if (n.label.equals("EXIT"))            color = "gray";
            else if (n.label.startsWith("if"))     color = "lightcyan";
            else if (n.label.startsWith("while"))  color = "lightsalmon";
            else if (n.label.startsWith("return")) color = "lightcoral";

            out.printf("  n%d [label=\"%s\", fillcolor=%s];%n",
                    n.id, label, color);
        }

        out.println();

        Set<String> drawn = new HashSet<>();
        for (CFGNode n : allNodes) {
            for (CFGNode succ : n.successors) {
                String key = n.id + "->" + succ.id;
                if (drawn.add(key)) {
                    out.printf("  n%d -> n%d;%n", n.id, succ.id);
                }
            }
        }

        out.println("}");
    }

    private String setToString(Set<Definition> set) {
        if (set == null || set.isEmpty()) return "{}";
        StringBuilder sb = new StringBuilder("{");
        for (Definition d : set) sb.append(d).append(" ");
        sb.append("}");
        return sb.toString();
    }
}