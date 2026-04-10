package ast;

public class IfStmt extends Node {
    public String condition;
    public Node thenBody;
    public Node elseBody;

    public IfStmt(String condition, Node thenBody, Node elseBody) {
        this.condition = condition;
        this.thenBody = thenBody;
        this.elseBody = elseBody;
    }

    public String toString() {
        return "if (" + condition + ")";
    }
}