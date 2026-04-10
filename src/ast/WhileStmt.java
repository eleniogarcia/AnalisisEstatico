package ast;

public class WhileStmt extends Node {
    public String condition;
    public Node body;

    public WhileStmt(String condition, Node body) {
        this.condition = condition;
        this.body = body;
    }

    public String toString() {
        return "while (" + condition + ")";
    }
}