import ast.*;
import cfg.*;
import parser.*;
import java.io.*;
import java.util.*;

public class Main {

    public static void main(String[] args) throws Exception {
        Reader input;
        String nombreArchivo = "ejemplo.txt";

        if (args.length > 0) {
            nombreArchivo = args[0];
            System.out.println("Leyendo archivo desde argumentos: " + nombreArchivo);
        } else {
            System.out.println("Leyendo archivo por defecto: " + nombreArchivo);
        }

        File file = new File(nombreArchivo);
        if (!file.exists()) {
            System.err.println("Error: No se encontró el archivo '" + nombreArchivo + "'");
            return;
        }
        input = new FileReader(file);

        // --- PARSEO ---
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

        PrintWriter pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream("cfg.dot"), "UTF-8"));
        new DotExporter().export(entry, pw);
        pw.close();
        generatePng("cfg.dot", "cfg.png");

        // Recolectar todos los nodos
        List<CFGNode> allNodes = getAllNodes(entry);
        if (!allNodes.contains(exitNode)) allNodes.add(exitNode);

        // --- PUNTO 2: Postdominadores ---
        DominanceAnalysis analysis = new DominanceAnalysis();
        analysis.computePostDominators(allNodes, exitNode);

        System.out.println("\n--- Postdominadores ---");
        for (CFGNode n : allNodes) {
            System.out.print("  pdom( " + n.label + " ) = { ");
            for (CFGNode pd : analysis.postDominators.get(n)) System.out.print(pd.label + "  ");
            System.out.println("}");
        }

        // --- PUNTO 3: Arbol de Postdominadores ---
        PostDominatorTree pdTree = new PostDominatorTree();
        pdTree.build(allNodes, analysis.postDominators, exitNode);
        pdTree.print(allNodes);

        PrintWriter pw3 = new PrintWriter(new OutputStreamWriter(new FileOutputStream("postdom_tree.dot"), "UTF-8"));
        pdTree.exportDot(allNodes, exitNode, pw3);
        pw3.close();
        generatePng("postdom_tree.dot", "postdom_tree.png");

        // --- PUNTO 4: Control Dependence Graph ---
        System.out.println("\n=== PUNTO 4: Control Dependence Graph ===");
        ControlDependenceGraph cdg = new ControlDependenceGraph();
        cdg.build(allNodes, analysis.postDominators, pdTree.iPostDom, entry, exitNode);
        cdg.print(allNodes);

        PrintWriter pw4 = new PrintWriter(new OutputStreamWriter(new FileOutputStream("cdg.dot"), "UTF-8"));
        cdg.exportDot(allNodes, pw4);
        pw4.close();
        generatePng("cdg.dot", "cdg.png");

        // --- PUNTO 5: Reaching Definitions ---
        System.out.println("\n=== PUNTO 5: Reaching Definitions ===");
        ReachingDefinitions rd = new ReachingDefinitions();
        rd.compute(allNodes);
        rd.print(allNodes);

        // --- PUNTO 6: Data Dependence Graph (DDG) ---
        System.out.println("\n=== PUNTO 6: Data Dependence Graph (DDG) ===");
        DataDependenceGraph ddg = new DataDependenceGraph();
        ddg.build(allNodes, rd);
        ddg.print(allNodes);

        PrintWriter pw6 = new PrintWriter(new OutputStreamWriter(new FileOutputStream("ddg.dot"), "UTF-8"));
        ddg.exportDot(allNodes, pw6);
        pw6.close();
        generatePng("ddg.dot", "ddg.png");

        System.out.println("\n✅ Proceso completado. Archivos .dot y .png generados.");
    }

    private static void generatePng(String dotFile, String pngFile) {
        try {
            String dotExe = "C:\\Program Files\\Graphviz\\bin\\dot.exe";
            ProcessBuilder pb = new ProcessBuilder(dotExe, "-Tpng", dotFile, "-o", pngFile);
            pb.redirectErrorStream(true);
            Process p = pb.start();
            p.waitFor();
            System.out.println("🖼️  " + pngFile + " generado.");
        } catch (Exception e) {
            System.out.println("Error generando " + pngFile + ": " + e.getMessage());
        }
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