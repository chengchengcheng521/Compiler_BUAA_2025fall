package error;


import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ErrorHandler {
    // 1. 单例实例
    private static final ErrorHandler instance = new ErrorHandler();

    private final List<Error> errors = new ArrayList<>();
    private final Set<Integer> lexicalErrorLines = new HashSet<>(); // 记录有词法错误的行

    // 2. 私有构造函数，防止外部实例化
    private ErrorHandler() {}

    // 3. 提供全局访问点
    public static ErrorHandler getInstance() {
        return instance;
    }

    // 4. 添加错误的方法
    public void addError(Error error) {
        this.errors.add(error);
        // 如果是词法错误，记录行号
        if (error.getType() == ErrorType.ILLEGAL_SYMBOL) {
            lexicalErrorLines.add(error.getLineNumber());
        }
    }

    // 重载方法，方便直接传入参数
    public void addError(int lineNumber, ErrorType type) {
        this.addError(new Error(lineNumber, type));
    }
    
    // 检查某一行是否有词法错误
    public boolean hasLexicalErrorOnLine(int lineNumber) {
        return lexicalErrorLines.contains(lineNumber);
    }

    // 5. 获取所有错误
    public List<Error> getErrors() {
        // 按行号排序
        errors.sort(Comparator.comparingInt(Error::getLineNumber));
        return errors;
    }

    // 6. 检查是否存在错误
    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    // (可选) 清空错误列表，用于测试
    public void clearErrors() {
        errors.clear();
    }
}