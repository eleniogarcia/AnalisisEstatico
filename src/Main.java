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

        // Resetear contador de nodos
        CFGNode.resetCounter();

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

        PrintWriter pw5 = new PrintWriter(new OutputStreamWriter(new FileOutputStream("reaching_def.dot"), "UTF-8"));
        rd.exportDot(allNodes, pw5);
        pw5.close();
        generatePng("reaching_def.dot", "reaching_def.png");

        // --- PUNTO 6: Data Dependence Graph (DDG) ---
        System.out.println("\n=== PUNTO 6: Data Dependence Graph (DDG) ===");
        DataDependenceGraph ddg = new DataDependenceGraph();
        ddg.build(allNodes, rd);
        ddg.print(allNodes);

        PrintWriter pw6 = new PrintWriter(new OutputStreamWriter(new FileOutputStream("ddg.dot"), "UTF-8"));
        ddg.exportDot(allNodes, pw6);
        pw6.close();
        generatePng("ddg.dot", "ddg.png");

        // --- PUNTO 7: Program Dependence Graph (PDG) ---
        System.out.println("\n=== PUNTO 7: Program Dependence Graph (PDG) ===");
        ProgramDependenceGraph pdg = new ProgramDependenceGraph();
        pdg.build(allNodes, cdg, ddg);

        PrintWriter pw7 = new PrintWriter(new OutputStreamWriter(new FileOutputStream("pdg.dot"), "UTF-8"));
        pdg.exportDot(allNodes, cdg, ddg, pw7);
        pw7.close();
        generatePng("pdg.dot", "pdg.png");
        System.out.println("PDG generado: pdg.dot / pdg.png");

        // --- PUNTO 8: Program Slicing interactivo ---
        System.out.println("\n=== PUNTO 8: Program Slicing (Backward Slice) ===");

        // Mostrar nodos disponibles (sin EXIT)
        List<CFGNode> sliceableNodes = new ArrayList<>();
        for (CFGNode n : allNodes) {
            if (!n.label.equals("EXIT")) sliceableNodes.add(n);
        }

        System.out.println("\nNodos disponibles para slicing:");
        for (int i = 0; i < sliceableNodes.size(); i++) {
            System.out.printf("  [%d] %s%n", i, sliceableNodes.get(i).label);
        }

        // Pedir al usuario que elija
        Scanner scanner = new Scanner(System.in);
        int eleccion = -1;
        while (eleccion < 0 || eleccion >= sliceableNodes.size()) {
            System.out.print("\nElegí el número del nodo criterio: ");
            try {
                eleccion = Integer.parseInt(scanner.nextLine().trim());
                if (eleccion < 0 || eleccion >= sliceableNodes.size()) {
                    System.out.println("Número inválido, intentá de nuevo.");
                }
            } catch (NumberFormatException e) {
                System.out.println("Ingresá un número válido.");
            }
        }

        CFGNode criterio = sliceableNodes.get(eleccion);

        ProgramSlicer slicer = new ProgramSlicer();
        slicer.compute(criterio, pdg);
        slicer.print();

        PrintWriter pw8 = new PrintWriter(new OutputStreamWriter(new FileOutputStream("slice.dot"), "UTF-8"));
        slicer.exportDot(allNodes, exitNode, pw8);
        pw8.close();
        generatePng("slice.dot", "slice.png");
        System.out.println("Slice generado: slice.dot / slice.png");

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