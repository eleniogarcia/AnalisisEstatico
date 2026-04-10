package ast;

public class ReturnStmt extends Node {
    public String expr;

    public ReturnStmt(String expr) {
        this.expr = expr;
    }

    public String toString() {
        return "return " + expr;
    }
}