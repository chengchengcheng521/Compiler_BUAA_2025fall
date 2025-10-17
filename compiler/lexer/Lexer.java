package lexer;

import error.ErrorHandler;
import error.ErrorType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Lexer {
    private final String source;
    private final List<Token> tokens = new ArrayList<>();
    private final ErrorHandler errorHandler = ErrorHandler.getInstance();


    private int position = 0;
    private int lineNumber = 1;

    private static final Map<String, TokenType> KEYWORDS;

    static {
        KEYWORDS = new HashMap<>();
        KEYWORDS.put("const", TokenType.CONSTTK);
        KEYWORDS.put("int", TokenType.INTTK);
        KEYWORDS.put("static", TokenType.STATICTK);
        KEYWORDS.put("break", TokenType.BREAKTK);
        KEYWORDS.put("continue", TokenType.CONTINUETK);
        KEYWORDS.put("if", TokenType.IFTK);
        KEYWORDS.put("else", TokenType.ELSETK);
        KEYWORDS.put("for", TokenType.FORTK);
        KEYWORDS.put("return", TokenType.RETURNTK);
        KEYWORDS.put("void", TokenType.VOIDTK);
        KEYWORDS.put("main", TokenType.MAINTK);
        KEYWORDS.put("printf", TokenType.PRINTFTK);
    }

    public Lexer(String source) {
        this.source = source;
    }

    public List<Token> getTokens() {
        return this.tokens;
    }

    // 主运行方法
    public void run() {
        while (!isAtEnd()) {
            scanToken();
        }
    }

    private void scanToken() {
        char c = advance();

        switch (c) {
            case '+': addToken(TokenType.PLUS, "+"); break;
            case '-': addToken(TokenType.MINU, "-"); break;
            case '*': addToken(TokenType.MULT, "*"); break;
            case '%': addToken(TokenType.MOD, "%"); break;
            case ';': addToken(TokenType.SEMICN, ";"); break;
            case ',': addToken(TokenType.COMMA, ","); break;
            case '(': addToken(TokenType.LPARENT, "("); break;
            case ')': addToken(TokenType.RPARENT, ")"); break;
            case '[': addToken(TokenType.LBRACK, "["); break;
            case ']': addToken(TokenType.RBRACK, "]"); break;
            case '{': addToken(TokenType.LBRACE, "{"); break;
            case '}': addToken(TokenType.RBRACE, "}"); break;

            case '!':
                if (match('=')) {
                    addToken(TokenType.NEQ, "!=");
                } else {
                    addToken(TokenType.NOT, "!");
                }
                break;
            case '=':
                if (match('=')) {
                    addToken(TokenType.EQL, "==");
                } else {
                    addToken(TokenType.ASSIGN, "=");
                }
                break;
            case '<':
                if (match('=')) {
                    addToken(TokenType.LEQ, "<=");
                } else {
                    addToken(TokenType.LSS, "<");
                }
                break;
            case '>':
                if (match('=')) {
                    addToken(TokenType.GEQ, ">=");
                } else {
                    addToken(TokenType.GRE, ">");
                }
                break;

            case '&':
                if (match('&')) {
                    addToken(TokenType.AND, "&&");
                } else {
                    // 报告非法符号错误
                    errorHandler.addError(lineNumber, ErrorType.ILLEGAL_SYMBOL);
                }
                break;
            case '|':
                if (match('|')) {
                    addToken(TokenType.OR, "||");
                } else {
                    // 报告非法符号错误
                    errorHandler.addError(lineNumber, ErrorType.ILLEGAL_SYMBOL);
                }
                break;

            case '/':
                if (match('/')) { // 单行注释
                    while (peek() != '\n' && !isAtEnd()) {
                        advance();
                    }
                } else if (match('*')) { // 多行注释
                    while (!(peek() == '*' && peekNext() == '/') && !isAtEnd()) {
                        if (peek() == '\n') {
                            lineNumber++;
                        }
                        advance();
                    }
                    if (!isAtEnd()) {
                        advance(); // consume '*'
                        advance(); // consume '/'
                    }
                    // 注意：如果多行注释未闭合直到文件末尾，这里不会报错，符合大多数编译器的行为
                } else { // 是除号
                    addToken(TokenType.DIV, "/");
                }
                break;

            // 忽略空白字符
            case ' ':
            case '\r':
            case '\t':
                break;

            // 处理换行
            case '\n':
                lineNumber++;
                break;

            // 字符串字面量
            case '"':
                scanString();
                break;

            default:
                if (isDigit(c)) {
                    // 如果是数字开头，扫描数字
                    scanNumber();
                } else if (isAlpha(c)) {
                    // 如果是字母或下划线开头，扫描标识符
                    scanIdentifier();
                } else {
                    // 对于所有其他无法识别的字符，都报告为非法符号错误
                    errorHandler.addError(lineNumber, ErrorType.ILLEGAL_SYMBOL);
                }
                break;
        }
    }

    // ===== 扫描复杂单词的私有方法 =====
    private void scanString() {
        int start = position;
        while (peek() != '"' && !isAtEnd()) {
            if (peek() == '\n') lineNumber++;
            advance();
        }
        if (isAtEnd()) {
            // TODO: 报告未闭合的字符串错误
            return;
        }
        advance();
        String value = source.substring(start - 1, position);
        addToken(TokenType.STRCON, value);
    }

    private void scanNumber() {
        int start = position - 1;
        while (isDigit(peek())) advance();
        String value = source.substring(start, position);
        addToken(TokenType.INTCON, value);
    }

    private void scanIdentifier() {
        int start = position - 1;
        while (isAlphaNumeric(peek())) advance();
        String text = source.substring(start, position);
        TokenType type = KEYWORDS.getOrDefault(text, TokenType.IDENFR);
        addToken(type, text);
    }

    // ===== 辅助工具方法 =====
    private boolean isAtEnd() { return position >= source.length(); }
    private char advance() { return source.charAt(position++); }
    private char peek() { return isAtEnd() ? '\0' : source.charAt(position); }
    private char peekNext() { return position + 1 >= source.length() ? '\0' : source.charAt(position + 1); }
    private boolean match(char expected) {
        if (isAtEnd() || source.charAt(position) != expected) return false;
        position++;
        return true;
    }
    private void addToken(TokenType type, String value) { tokens.add(new Token(type, value, lineNumber)); }
    private boolean isDigit(char c) { return c >= '0' && c <= '9'; }
    private boolean isAlpha(char c) { return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || c == '_'; }
    private boolean isAlphaNumeric(char c) { return isAlpha(c) || isDigit(c); }
}