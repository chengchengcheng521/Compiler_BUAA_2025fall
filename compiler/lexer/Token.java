package lexer;

public class Token {
    private final TokenType type;    // 单词类别码
    private final String value;      // 单词的原始字符串
    private final int lineNumber;    // 单词所在的行号

    public Token(TokenType type, String value, int lineNumber) {
        this.type = type;
        this.value = value;
        this.lineNumber = lineNumber;
    }

    public TokenType getType() {
        return type;
    }

    public String getValue() {
        return value;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    // 重写toString()方法，使其完全符合 "类别码 单词串" 的输出格式
    @Override
    public String toString() {
        return type.name() + " " + value;
    }
}