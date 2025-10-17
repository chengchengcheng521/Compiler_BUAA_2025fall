package error;

public enum ErrorType {
    // 词法错误
    ILLEGAL_SYMBOL('a'), // 文法中未定义的非法符号，我们暂时也归为'a'

    // 语法错误 (先定义出来，后面用)
    MISSING_SEMICN('i'),
    MISSING_RPARENT('j'),
    MISSING_RBRACK('k'),

    // 语义错误 (先定义出来，后面用)
    NAME_REDIFINED('b'),
    NAME_UNDEFINED('c'),
    PARAM_NUM_MISMATCH('d'),
    PARAM_TYPE_MISMATCH('e'),
    VOID_FUNC_RETURN_VALUE('f'),
    NON_VOID_FUNC_NO_RETURN('g'),
    MODIFY_CONST('h'),
    PRINTF_ARG_NUM_MISMATCH('l'),
    BREAK_CONTINUE_OUT_OF_LOOP('m');

    private final char code;

    ErrorType(char code) {
        this.code = code;
    }

    public char getCode() {
        return code;
    }
}