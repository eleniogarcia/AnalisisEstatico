package ast;

public class AssignStmt extends Node {
    public String id;
    public String expr;

    public AssignStmt(String id, String expr) {
        this.id = id;
        this.expr = expr;
    }

    public String toString() {
        return id + " := " + expr;
    }
}