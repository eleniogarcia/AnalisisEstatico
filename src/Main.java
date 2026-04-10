import ast.*;
import cfg.*;
import parser.*;
import java.io.*;
import java.util.*;

public class Main {

    public static void main(String[] args) throws Exception {
        Reader input;
        String nombreArchivo = "ejemplo.txt"; // Nombre por defecto

        // Lógica para decidir qué archivo leer
        if (args.length > 0) {
            nombreArchivo = args[0];
            System.out.println("Leyendo archivo desde argumentos: " + nombreArchivo);
        } else {
            System.out.println("Leyendo archivo por defecto: " + nombreArchivo);
        }

        File file = new File(nombreArchivo);
        if (!file.exists()) {
            System.err.println("Error: No se encontró el archivo '" + nombreArchivo + "' en la raíz del proyecto.");
            return;
        }

        input = new FileReader(file);

        // --- PROCESO DE PARSEO ---
        Lexer lexer = new Lexer(input);
        Parser parser = new Parser(lexer);
        Node programa;
        try {
            programa = (Node) parser.parse().value;
            System.out.println("✅ Parseo exitoso del archivo .txt");
        } catch (Exception e) {
            System.err.println("❌ Error durante el parseo: " + e.getMessage());
            throw e;
        }

        // --- PUNTO 1: CFG ---
        CFGBuilder builder = new CFGBuilder();
        CFGNode[] result = builder.build(programa);
        CFGNode entry = result[0];
        CFGNode exitNode = new CFGNode("EXIT");
        for (int i = 1; i < result.length; i++) result[i].addEdge(exitNode);

        // Exportar Grafo (Punto 1)
        PrintWriter pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream("cfg.dot"), "UTF-8"));
        new DotExporter().export(entry, pw);
        pw.close();

        // --- PUNTO 2: Postdominadores ---
        List<CFGNode> allNodes = getAllNodes(entry);
        if (!allNodes.contains(exitNode)) allNodes.add(exitNode);
        DominanceAnalysis analysis = new DominanceAnalysis();
        analysis.computePostDominators(allNodes, exitNode);

        // --- PUNTO 3: Árbol de Postdominadores ---
        PostDominatorTree pdTree = new PostDominatorTree();
        pdTree.build(allNodes, analysis.postDominators, exitNode);
        pdTree.print(allNodes); // Imprime en consola el IPDom de cada nodo

        PrintWriter pw3 = new PrintWriter(new OutputStreamWriter(new FileOutputStream("postdom_tree.dot"), "UTF-8"));
        pdTree.exportDot(allNodes, exitNode, pw3);
        pw3.close();

        System.out.println("\n✅ Proceso completado. Archivos .dot generados.");
    }

    private static List<CFGNode> getAllNodes(CFGNode start) {
        Set<CFGNode> visited = new LinkedHashSet<>();
        Deque<CFGNode> stack = new ArrayDeque<>();
        stack.push(start);
        while (!stack.isEmpty()) {
            CFGNode n = stack.pop();
            if (visited.add(n)) for (CFGNode s : n.successors) stack.push(s);
        }
        return new ArrayList<>(visited);
    }
}