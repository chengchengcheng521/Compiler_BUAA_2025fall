package error;


public class Error {
    private final int lineNumber;
    private final ErrorType type;

    public Error(int lineNumber, ErrorType type) {
        this.lineNumber = lineNumber;
        this.type = type;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public ErrorType getType() {
        return type;
    }

    // 格式化为 "行号 错误码"
    @Override
    public String toString() {
        return lineNumber + " " + type.getCode();
    }
}