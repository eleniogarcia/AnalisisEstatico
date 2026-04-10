package ast;

public class SeqStmt extends Node {
    public Node first;
    public Node second;

    public SeqStmt(Node first, Node second) {
        this.first = first;
        this.second = second;
    }

    public String toString() {
        return "seq";    }
}