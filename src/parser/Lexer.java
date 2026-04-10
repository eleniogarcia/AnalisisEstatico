package parser;

import java_cup.runtime.Symbol;
import java.io.Reader;
import java.io.IOException;

public class Lexer implements java_cup.runtime.Scanner {

    private Reader input;
    private int    current = -2;
    private int    line    = 0;
    private int    column  = 0;

    public Lexer(Reader input) {
        this.input = input;
    }

    private int nextChar() throws IOException {
        int c = input.read();
        if (c == '\n') { line++; column = 0; }
        else           { column++; }
        return c;
    }

    private int peek() throws IOException {
        if (current == -2) current = nextChar();
        return current;
    }

    private int consume() throws IOException {
        int c = peek();
        current = -2;
        return c;
    }

    private Symbol tok(int type) {
        return new Symbol(type, line, column);
    }
    private Symbol tok(int type, Object value) {
        return new Symbol(type, line, column, value);
    }

    public Symbol next_token() throws Exception {
        while (true) {
            int c = peek();
            if (c == -1) return tok(sym.EOF);
            if (Character.isWhitespace(c)) { consume(); continue; }
            if (Character.isDigit(c)) {
                StringBuilder sb = new StringBuilder();
                while (peek() != -1 && Character.isDigit(peek())) sb.append((char) consume());
                return tok(sym.NUMBER, sb.toString());
            }
            if (Character.isLetter(c) || c == '_') {
                StringBuilder sb = new StringBuilder();
                while (peek() != -1 && (Character.isLetterOrDigit(peek()) || peek() == '_')) sb.append((char) consume());
                String word = sb.toString();
                switch (word) {
                    case "integer": return tok(sym.INTEGER);
                    case "if":      return tok(sym.IF);
                    case "else":    return tok(sym.ELSE);
                    case "while":   return tok(sym.WHILE);
                    case "return":  return tok(sym.RETURN);
                    default:        return tok(sym.ID, word);
                }
            }
            consume();
            switch (c) {
                case '+': return tok(sym.PLUS);
                case '=': return tok(sym.ASSIGN);
                case ';': return tok(sym.SEMI);
                case '(': return tok(sym.LPAREN);
                case ')': return tok(sym.RPAREN);
                case '{': return tok(sym.LBRACE);
                case '}': return tok(sym.RBRACE);
                default: throw new RuntimeException("Caracter inesperado: '" + (char)c + "' en linea " + line);
            }
        }
    }
}
