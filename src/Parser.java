import java.util.List;

public class Parser {
    private List<Token> tokens;
    private int pos;
    private ValidationResult result;

    public SelectStatement parse(List<Token> tokens, ValidationResult result) {
        this.tokens = tokens;
        this.pos = 0;
        this.result = result;
        SelectStatement statement = new SelectStatement();
        expect(TokenType.SELECT, "SYNTACTIC_EXPECTED_SELECT");
        parseColumns(statement);
        expect(TokenType.FROM, "SYNTACTIC_EXPECTED_FROM");
        Token table = expect(TokenType.IDENTIFIER, "SYNTACTIC_EXPECTED_TABLE");
        if (table != null) statement.table = table.lexeme;

        // TODO SERIE 2:
        // Implementar parseo de WHERE opcional:
        // WHERE <columna> <operador> <literal> (AND|OR <columna> <operador> <literal>)*
        // Debe llenar statement.where con SourceSpan exactos.
        if (match(TokenType.WHERE)) {
    statement.where = new ConditionChain();

    WhereCondition first = parseWhereCondition();
    if (first != null) {
        statement.where.conditions.add(first);
    }

    while (match(TokenType.AND) || match(TokenType.OR)) {
        Token connector = tokens.get(pos - 1);
        statement.where.connectors.add(connector.type.name());

        WhereCondition next = parseWhereCondition();
        if (next != null) {
            statement.where.conditions.add(next);
        }
    }
}

        if (check(TokenType.SEMICOLON)) advance();
        if (!check(TokenType.EOF)) {
            result.diagnostics.add(new Diagnostic("SYNTACTIC_UNEXPECTED_TOKEN", "Token inesperado: " + current().lexeme, current().span));
        }
        return statement;
    }

    private void parseColumns(SelectStatement statement) {
        if (match(TokenType.STAR)) {
            statement.columns.add("*");
            return;
        }
        Token first = expect(TokenType.IDENTIFIER, "SYNTACTIC_EXPECTED_COLUMN");
        if (first != null) statement.columns.add(first.lexeme);
        while (match(TokenType.COMMA)) {
            Token next = expect(TokenType.IDENTIFIER, "SYNTACTIC_EXPECTED_COLUMN");
            if (next != null) statement.columns.add(next.lexeme);
        }
    }

    private Token expect(TokenType type, String code) {
        if (check(type)) return advance();
        result.diagnostics.add(new Diagnostic(code, "Se esperaba " + type + " y se encontró " + current().type, current().span));
        return null;
    }

    private boolean match(TokenType type) { if (check(type)) { advance(); return true; } return false; }
    private boolean check(TokenType type) { return current().type == type; }
    private Token current() { return tokens.get(pos); }
    private Token advance() { return tokens.get(pos++); }

    private WhereCondition parseWhereCondition() {

    Token column = expect(TokenType.IDENTIFIER,
            "SYNTACTIC_EXPECTED_WHERE_OPERAND");

    if (column == null) {
        return null;
    }

    Token operator = parseOperator();

    if (operator == null) {
        result.diagnostics.add(new Diagnostic(
                "SYNTACTIC_EXPECTED_WHERE_OPERAND",
                "Operador WHERE esperado",
                current().span));
        return null;
    }

    Token literal = parseLiteral();

    if (literal == null) {
        result.diagnostics.add(new Diagnostic(
                "SYNTACTIC_EXPECTED_WHERE_OPERAND",
                "Operando WHERE esperado",
                current().span));
        return null;
    }

    LiteralType type = LiteralType.UNKNOWN;

    if (literal.type == TokenType.NUMBER) {
        type = LiteralType.NUMBER;
    } else if (literal.type == TokenType.STRING) {
        type = LiteralType.STRING;
    } else if (literal.type == TokenType.TRUE ||
               literal.type == TokenType.FALSE) {
        type = LiteralType.BOOLEAN;
    }

    return new WhereCondition(
            column.lexeme,
            operator.lexeme,
            literal.lexeme,
            type,
            column.span,
            operator.span,
            literal.span
    );
}

private Token parseOperator() {
    if (match(TokenType.EQUAL)) return tokens.get(pos - 1);
    if (match(TokenType.GREATER)) return tokens.get(pos - 1);
    if (match(TokenType.LESS)) return tokens.get(pos - 1);
    if (match(TokenType.GREATER_EQUAL)) return tokens.get(pos - 1);
    if (match(TokenType.LESS_EQUAL)) return tokens.get(pos - 1);
    if (match(TokenType.NOT_EQUAL)) return tokens.get(pos - 1);
    return null;
}

private Token parseLiteral() {
    if (match(TokenType.NUMBER)) return tokens.get(pos - 1);
    if (match(TokenType.STRING)) return tokens.get(pos - 1);
    if (match(TokenType.TRUE)) return tokens.get(pos - 1);
    if (match(TokenType.FALSE)) return tokens.get(pos - 1);
    return null;
}
}
