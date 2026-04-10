package cfg;
import java.util.*;

public class DominanceAnalysis {
    // Aquí guardamos: Nodo -> Conjunto de sus Postdominadores
    public Map<CFGNode, Set<CFGNode>> postDominators = new HashMap<>();

    public void computePostDominators(List<CFGNode> allNodes, CFGNode exitNode) {
        // 1. Inicialización
        for (CFGNode n : allNodes) {
            Set<CFGNode> initial = new HashSet<>(allNodes);
            if (n == exitNode) {
                initial.clear();
                initial.add(exitNode);
            }
            postDominators.put(n, initial);
        }

        // 2. Algoritmo Iterativo (Punto Fijo)
        boolean changed = true;
        while (changed) {
            changed = false;
            for (CFGNode n : allNodes) {
                if (n == exitNode) continue;

                Set<CFGNode> newPDom = null;
                // Los postdominadores se calculan mirando los SUCESORES
                for (CFGNode succ : n.successors) {
                    if (newPDom == null) {
                        newPDom = new HashSet<>(postDominators.get(succ));
                    } else {
                        newPDom.retainAll(postDominators.get(succ));
                    }
                }

                if (newPDom == null) newPDom = new HashSet<>();
                newPDom.add(n); // Un nodo siempre se post-domina a sí mismo

                if (!newPDom.equals(postDominators.get(n))) {
                    postDominators.put(n, newPDom);
                    changed = true;
                }
            }
        }
    }
}