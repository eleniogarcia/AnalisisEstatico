package cfg;

import ast.*;

public class CFGBuilder {

    public CFGNode[] build(Node stmt) {

        if (stmt instanceof AssignStmt) {
            AssignStmt assign = (AssignStmt) stmt;
            CFGNode n = new CFGNode(assign.toString(), assign);
            return new CFGNode[]{n, n};
        }
        if (stmt instanceof ReturnStmt) {
            ReturnStmt ret = (ReturnStmt) stmt;
            CFGNode n = new CFGNode(ret.toString(), ret);
            return new CFGNode[]{n, n};
        }
        if (stmt instanceof IfStmt) {
            IfStmt ifStmt = (IfStmt) stmt;
            CFGNode cond = new CFGNode("if (" + ifStmt.condition + ")", ifStmt);
            CFGNode[] thenB = build(ifStmt.thenBody);
            CFGNode[] elseB = build(ifStmt.elseBody);
            cond.addEdge(thenB[0]);
            cond.addEdge(elseB[0]);
            CFGNode[] result = new CFGNode[thenB.length + elseB.length - 1];
            result[0] = cond;
            System.arraycopy(thenB, 1, result, 1, thenB.length - 1);
            System.arraycopy(elseB, 1, result, thenB.length, elseB.length - 1);
            return result;
        }
        if (stmt instanceof WhileStmt) {
            WhileStmt whileStmt = (WhileStmt) stmt;
            CFGNode cond = new CFGNode("while (" + whileStmt.condition + ")", whileStmt);
            CFGNode[] body = build(whileStmt.body);
            cond.addEdge(body[0]);
            for (int i = 1; i < body.length; i++) body[i].addEdge(cond);
            return new CFGNode[]{cond, cond};
        }
        if (stmt instanceof SeqStmt) {
            SeqStmt seq = (SeqStmt) stmt;
            CFGNode[] left  = build(seq.first);
            CFGNode[] right = build(seq.second);
            for (int i = 1; i < left.length; i++) left[i].addEdge(right[0]);
            CFGNode[] result = new CFGNode[right.length];
            result[0] = left[0];
            System.arraycopy(right, 1, result, 1, right.length - 1);
            return result;
        }
        throw new RuntimeException("Nodo AST no soportado: " + stmt.getClass().getName());
    }
}
