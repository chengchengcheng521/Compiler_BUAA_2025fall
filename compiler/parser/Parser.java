package parser;

import error.ErrorHandler;
import error.ErrorType;
import java.io.PrintWriter;
import java.util.List;
import lexer.Token;
import lexer.TokenType;

public class Parser {
    private final List<Token> tokens;
    private int position = 0;
    private final ErrorHandler errorHandler;
    private final PrintWriter outputWriter;

    public Parser(List<Token> tokens, PrintWriter writer) {
        this.tokens = tokens;
        this.errorHandler = ErrorHandler.getInstance();
        this.outputWriter = writer;
    }

    // ===============================================================
    // ==================== 1. 核心辅助方法 ============================
    // ===============================================================

    private Token peek() {
        if (position < tokens.size()) {
            return tokens.get(position);
        }
        // 返回一个文件末尾的标记，避免空指针
        // 使用列表中最后一个token的行号，这在报告文件末尾的错误时很有用
        int lastLine = tokens.isEmpty() ? 1 : tokens.get(tokens.size() - 1).getLineNumber();
        return new Token(null, "EOF", lastLine);
    }

    // 预读辅助方法，offset=0表示当前token，offset=1表示下一个token，以此类推
    private Token peek(int offset) {
        int index = position + offset;
        if (index < tokens.size()) {
            return tokens.get(index);
        }
        // 使用列表中最后一个token的行号，这在报告文件末尾的错误时很有用
        int lastLine = tokens.isEmpty() ? 1 : tokens.get(tokens.size() - 1).getLineNumber();
        return new Token(null, "EOF", lastLine);
    }

    private Token consume() {
        Token current = peek();
        if (outputWriter != null && current.getType() != null) { // 不打印EOF
            outputWriter.println(current.toString());
        }
        if (position < tokens.size()) { // 防止指针越界
            position++;
        }
        return current;
    }

    private boolean match(TokenType... types) {
        for (TokenType type : types) {
            if (peek().getType() == type) {
                consume();
                return true;
            }
        }
        return false;
    }

    private Token expect(TokenType type, ErrorType errorType) {
        return expect(type, errorType, false);
    }

    private Token expect(TokenType type, ErrorType errorType, boolean skipToTarget) {
        if (peek().getType() == type) {
            return consume();
        }
        // 错误！上一个token的行号通常是最好的错误位置指示
        int errorLine = (position > 0) ? tokens.get(position - 1).getLineNumber() : peek().getLineNumber();
        if (errorType != null) {
            // 只有在该行没有词法错误时才报告语法错误
            if (!errorHandler.hasLexicalErrorOnLine(errorLine)) {
                errorHandler.addError(errorLine, errorType);
            }
        }
        
        // 错误恢复：跳过token直到找到期望的token
        if (skipToTarget) {
            while (peek().getType() != type && peek().getType() != null) {
                // 也要避免跳过重要的结构符号
                // RBRACE, SEMICN 是语句结束标志，不应跳过
                // LBRACE 是新代码块开始，也不应跳过
                if (peek().getType() == TokenType.RBRACE || 
                    peek().getType() == TokenType.SEMICN ||
                    peek().getType() == TokenType.LBRACE) {
                    break;
                }
                consume();
            }
            if (peek().getType() == type) {
                return consume();
            }
        }
        
        return null;
    }

    private void printNonTerminal(String name) {
        if (outputWriter != null) {
            outputWriter.println("<" + name + ">");
        }
    }


    // ===============================================================
    // ==================== 2. 顶层结构解析 ===========================
    // ===============================================================

    public void parse() {
        parseCompUnit();
    }

    // CompUnit -> {Decl} {FuncDef} MainFuncDef
    // CompUnit -> {Decl} {FuncDef} MainFuncDef
    private void parseCompUnit() {
        // {Decl} 部分: 预读第3个token来区分变量/常量声明和函数定义
        while (peek().getType() == TokenType.CONSTTK ||
                (peek().getType() == TokenType.INTTK && peek(2).getType() != TokenType.LPARENT)) {
            parseDecl();
        }

        // {FuncDef} 部分: 预读第2个token来区分普通函数和main函数
        while (peek().getType() == TokenType.VOIDTK ||
                (peek().getType() == TokenType.INTTK && peek(1).getType() != TokenType.MAINTK)) {
            parseFuncDef();
        }

        // MainFuncDef 部分
        if (peek().getType() == TokenType.INTTK && peek(1).getType() == TokenType.MAINTK) {
            parseMainFuncDef();
        }

        printNonTerminal("CompUnit");
    }


    // ===============================================================
    // ==================== 3. 声明解析 ==============================
    // ===============================================================

    // Decl -> ConstDecl | VarDecl
    private void parseDecl() {
        if (peek().getType() == TokenType.CONSTTK) {
            parseConstDecl();
        } else {
            parseVarDecl();
        }
    }

    // ConstDecl -> 'const' BType ConstDef { ',' ConstDef } ';'
    private void parseConstDecl() {
        consume(); // 'const'
        parseBType();
        parseConstDef();
        while (match(TokenType.COMMA)) {
            parseConstDef();
        }
        expect(TokenType.SEMICN, ErrorType.MISSING_SEMICN);
        printNonTerminal("ConstDecl");
    }

    // VarDecl -> [ 'static' ] BType VarDef { ',' VarDef } ';'
    private void parseVarDecl() {
        match(TokenType.STATICTK); // [ 'static' ]
        parseBType();
        parseVarDef();
        while (match(TokenType.COMMA)) {
            parseVarDef();
        }
        expect(TokenType.SEMICN, ErrorType.MISSING_SEMICN);
        printNonTerminal("VarDecl");
    }

    // ConstDef -> Ident [ '[' ConstExp ']' ] '=' ConstInitVal
    private void parseConstDef() {
        match(TokenType.IDENFR);
        if (match(TokenType.LBRACK)) {
            parseConstExp();
            expect(TokenType.RBRACK, ErrorType.MISSING_RBRACK);
        }
        expect(TokenType.ASSIGN, null); // 在此上下文中，'='缺失是更严重的结构错误
        parseConstInitVal();
        printNonTerminal("ConstDef");
    }

    // VarDef -> Ident [ '[' ConstExp ']' ] | Ident [ '[' ConstExp ']' ] '=' InitVal
    private void parseVarDef() {
        match(TokenType.IDENFR);
        if (match(TokenType.LBRACK)) {
            parseConstExp();
            expect(TokenType.RBRACK, ErrorType.MISSING_RBRACK);
        }
        if (match(TokenType.ASSIGN)) {
            parseInitVal();
        }
        printNonTerminal("VarDef");
    }

    // BType -> 'int' (不输出)
    private void parseBType() {
        match(TokenType.INTTK);
    }


    // ===============================================================
    // ==================== 4. 函数解析 ==============================
    // ===============================================================

    // FuncDef -> FuncType Ident '(' [FuncFParams] ')' Block
    private void parseFuncDef() {
        parseFuncType();
        match(TokenType.IDENFR);
        expect(TokenType.LPARENT, null);
        if (peek().getType() != TokenType.RPARENT) {
            parseFuncFParams();
        }
        expect(TokenType.RPARENT, ErrorType.MISSING_RPARENT);
        parseBlock();
        printNonTerminal("FuncDef");
    }

    // MainFuncDef -> 'int' 'main' '(' ')' Block
    private void parseMainFuncDef() {
        match(TokenType.INTTK);
        match(TokenType.MAINTK);
        expect(TokenType.LPARENT, null);
        expect(TokenType.RPARENT, ErrorType.MISSING_RPARENT);
        parseBlock();
        printNonTerminal("MainFuncDef");
    }

    // FuncType -> 'void' | 'int'
    private void parseFuncType() {
        if (peek().getType() == TokenType.VOIDTK || peek().getType() == TokenType.INTTK) {
            consume();
        } else {
            // 错误：无效的函数返回类型，由上层调用者处理
        }
        printNonTerminal("FuncType");
    }

    // FuncFParams -> FuncFParam { ',' FuncFParam }
    private void parseFuncFParams() {
        parseFuncFParam();
        while (match(TokenType.COMMA)) {
            parseFuncFParam();
        }
        printNonTerminal("FuncFParams");
    }

    // FuncFParam -> BType Ident ['[' ']']
    private void parseFuncFParam() {
        parseBType();
        match(TokenType.IDENFR);
        if (match(TokenType.LBRACK)) {
            expect(TokenType.RBRACK, ErrorType.MISSING_RBRACK);
        }
        printNonTerminal("FuncFParam");
    }


    // ===============================================================
    // ==================== 5. 语句解析 ==============================
    // ===============================================================

    // Block -> '{' { BlockItem } '}'
    private void parseBlock() {
        expect(TokenType.LBRACE, null);
        while (peek().getType() != TokenType.RBRACE && peek().getType() != null) {
            int beforePos = position; // 记录解析前的位置
            parseBlockItem();
            // 恐慌模式：如果没有消费任何token，强制跳过当前token防止无限循环
            if (position == beforePos && peek().getType() != TokenType.RBRACE && peek().getType() != null) {
                consume(); // 强制消费一个token
            }
        }
        expect(TokenType.RBRACE, null);
        printNonTerminal("Block");
    }

    // BlockItem -> Decl | Stmt (不输出)
    private void parseBlockItem() {
        if (peek().getType() == TokenType.CONSTTK || 
            peek().getType() == TokenType.INTTK || 
            peek().getType() == TokenType.STATICTK) {
            parseDecl();
        } else {
            parseStmt();
        }
    }
    // ConstInitVal -> ConstExp | '{' [ ConstExp { ',' ConstExp } ] '}'
    private void parseConstInitVal() {
        if (peek().getType() == TokenType.LBRACE) {
            consume(); // '{'
            if (peek().getType() != TokenType.RBRACE) {
                parseConstExp();
                while (match(TokenType.COMMA)) {
                    parseConstExp();
                }
            }
            expect(TokenType.RBRACE, null); // '}'
        } else {
            parseConstExp();
        }
        printNonTerminal("ConstInitVal");
    }
    // InitVal -> Exp | '{' [ Exp { ',' Exp } ] '}'
    private void parseInitVal() {
        if (peek().getType() == TokenType.LBRACE) {
            consume(); // '{'
            if (peek().getType() != TokenType.RBRACE) {
                parseExp();
                while (match(TokenType.COMMA)) {
                    parseExp();
                }
            }
            expect(TokenType.RBRACE, null); // '}'
        } else {
            parseExp();
        }
        printNonTerminal("InitVal");
    }

    // Stmt -> ... (多种情况)
    private void parseStmt() {
        TokenType currentType = peek().getType();

        switch (currentType) {
            case IFTK: // 'if' Stmt
                consume(); // 'if'
                expect(TokenType.LPARENT, null);
                parseCond();
                expect(TokenType.RPARENT, ErrorType.MISSING_RPARENT, true); // 启用错误恢复
                parseStmt();
                if (match(TokenType.ELSETK)) { // [ 'else' Stmt ]
                    parseStmt();
                }
                printNonTerminal("Stmt");
                break;

            case FORTK: // 'for' Stmt
                // 'for' '(' [ForStmt] ';' [Cond] ';' [ForStmt] ')' Stmt
                consume(); // 'for'
                expect(TokenType.LPARENT, null);
                if (peek().getType() != TokenType.SEMICN) parseForStmt();
                expect(TokenType.SEMICN, null); // 第一个分号
                if (peek().getType() != TokenType.SEMICN) parseCond();
                expect(TokenType.SEMICN, null); // 第二个分号
                if (peek().getType() != TokenType.RPARENT) parseForStmt();
                expect(TokenType.RPARENT, ErrorType.MISSING_RPARENT, true); // 启用错误恢复
                parseStmt();
                printNonTerminal("Stmt");
                break;

            case BREAKTK:
            case CONTINUETK:
                consume(); // 'break' or 'continue'
                expect(TokenType.SEMICN, ErrorType.MISSING_SEMICN);
                printNonTerminal("Stmt");
                break;

            case RETURNTK:
                consume(); // 'return'
                // [Exp] 部分，如果不是分号，说明有返回值表达式
                if (peek().getType() != TokenType.SEMICN) {
                    parseExp();
                }
                expect(TokenType.SEMICN, ErrorType.MISSING_SEMICN);
                printNonTerminal("Stmt");
                break;

            case PRINTFTK:
                consume(); // 'printf'
                expect(TokenType.LPARENT, null);
                expect(TokenType.STRCON, null); // 必须有一个字符串
                while(match(TokenType.COMMA)) {
                    parseExp();
                }
                expect(TokenType.RPARENT, ErrorType.MISSING_RPARENT);
                expect(TokenType.SEMICN, ErrorType.MISSING_SEMICN);
                printNonTerminal("Stmt");
                break;

            case LBRACE: // Block
                parseBlock();
                printNonTerminal("Stmt"); // Block作为语句，需要输出Stmt
                break;

            case SEMICN: // 空语句 [Exp];
                consume();
                printNonTerminal("Stmt");
                break;

            default: // 剩下的情况是 LVal = Exp; 或 [Exp];
                // 这是一个难点：如何区分 a = 1; 和 a + 1; ？
                // 我们需要一直向前看到 '='。如果能看到，就是赋值语句。
                // 这是一个简化的判断，对于 a[b=1] = 2; 可能会出错，但对SysY够用
                if (peek().getType() == TokenType.IDENFR) {
                    if (isAssignStmt()) {
                        // LVal '=' Exp ';'
                        parseLVal();
                        expect(TokenType.ASSIGN, null);
                        parseExp();
                        expect(TokenType.SEMICN, ErrorType.MISSING_SEMICN);
                    } else {
                        // [Exp] ';'
                        parseExp();
                        expect(TokenType.SEMICN, ErrorType.MISSING_SEMICN);
                    }
                } else if (peek().getType() != null) {
                    // 意外的token，尝试解析为表达式（如果失败会由上层的恐慌模式处理）
                    parseExp();
                    expect(TokenType.SEMICN, ErrorType.MISSING_SEMICN);
                } else {
                    // EOF，跳出
                }
                printNonTerminal("Stmt");
                break;
        }

        // 所有Stmt类型都应该在各自的case中输出Stmt
    }

    // 辅助方法，判断当前是否为赋值语句
    private boolean isAssignStmt() {
        int tempPos = position;
        // 扫描直到遇到 ';' 或 '='
        while (tempPos < tokens.size()) {
            TokenType type = tokens.get(tempPos).getType();
            if (type == TokenType.SEMICN) {
                return false; // 先遇到分号，不是赋值语句
            }
            if (type == TokenType.ASSIGN) {
                return true; // 先遇到等号，是赋值语句
            }
            tempPos++;
        }
        return false;
    }

    // ForStmt -> LVal '=' Exp { ',' LVal '=' Exp }
    private void parseForStmt() {
        parseLVal();
        expect(TokenType.ASSIGN, null);
        parseExp();
        while (match(TokenType.COMMA)) {
            parseLVal();
            expect(TokenType.ASSIGN, null);
            parseExp();
        }
        printNonTerminal("ForStmt");
    }





// ===============================================================
// ==================== 6. 表达式解析 ============================
// ===============================================================

    // LVal -> Ident ['[' Exp ']']
    private void parseLVal() {
        expect(TokenType.IDENFR, null);
        if (match(TokenType.LBRACK)) {
            parseExp();
            expect(TokenType.RBRACK, ErrorType.MISSING_RBRACK);
        }
        printNonTerminal("LVal");
    }

    // PrimaryExp -> '(' Exp ')' | LVal | Number
    private void parsePrimaryExp() {
        if (match(TokenType.LPARENT)) {
            parseExp();
            expect(TokenType.RPARENT, ErrorType.MISSING_RPARENT);
        } else if (peek().getType() == TokenType.INTCON) {
            parseNumber();
        } else {
            parseLVal();
        }
        printNonTerminal("PrimaryExp");
    }

    // Number -> IntConst
    private void parseNumber() {
        expect(TokenType.INTCON, null);
        printNonTerminal("Number");
    }

    // UnaryExp -> PrimaryExp | Ident '(' [FuncRParams] ')' | UnaryOp UnaryExp
    private void parseUnaryExp() {
        TokenType type = peek().getType();
        if (type == TokenType.PLUS || type == TokenType.MINU || type == TokenType.NOT) {
            parseUnaryOp();
            parseUnaryExp();
        } else if (type == TokenType.IDENFR && peek(1).getType() == TokenType.LPARENT) {
            consume(); // Ident
            consume(); // '('
            if (peek().getType() != TokenType.RPARENT) {
                parseFuncRParams();
            }
            expect(TokenType.RPARENT, ErrorType.MISSING_RPARENT);
        } else {
            parsePrimaryExp();
        }
        printNonTerminal("UnaryExp");
    }

    // UnaryOp -> '+' | '-' | '!'
    private void parseUnaryOp() {
        match(TokenType.PLUS, TokenType.MINU, TokenType.NOT);
        printNonTerminal("UnaryOp");
    }

    // FuncRParams -> Exp { ',' Exp }
    private void parseFuncRParams() {
        parseExp();
        while (match(TokenType.COMMA)) {
            parseExp();
        }
        printNonTerminal("FuncRParams");
    }

    // -- 左结合表达式的解析 --

    // MulExp -> UnaryExp { ('*' | '/' | '%') UnaryExp }
    private void parseMulExp() {
        parseUnaryExp();
        printNonTerminal("MulExp");
        while (peek().getType() == TokenType.MULT ||
                peek().getType() == TokenType.DIV ||
                peek().getType() == TokenType.MOD) {
            consume();
            parseUnaryExp();
            printNonTerminal("MulExp");
        }
    }

    // AddExp -> MulExp { ('+' | '-') MulExp }
    private void parseAddExp() {
        parseMulExp();
        printNonTerminal("AddExp");
        while (peek().getType() == TokenType.PLUS ||
                peek().getType() == TokenType.MINU) {
            consume();
            parseMulExp();
            printNonTerminal("AddExp");
        }
    }

    // RelExp -> AddExp { ('<' | '>' | '<=' | '>=') AddExp }
    private void parseRelExp() {
        parseAddExp();
        while (peek().getType() == TokenType.LSS ||
                peek().getType() == TokenType.LEQ ||
                peek().getType() == TokenType.GRE ||
                peek().getType() == TokenType.GEQ) {
            printNonTerminal("RelExp");
            consume();
            parseAddExp();
        }
        printNonTerminal("RelExp");
    }

    // EqExp -> RelExp { ('==' | '!=') RelExp }
    private void parseEqExp() {
        parseRelExp();
        printNonTerminal("EqExp");
        while (peek().getType() == TokenType.EQL ||
                peek().getType() == TokenType.NEQ) {
            consume();
            parseRelExp();
            printNonTerminal("EqExp");
        }
    }

    // LAndExp -> EqExp { '&&' EqExp }
    private void parseLAndExp() {
        parseEqExp();
        printNonTerminal("LAndExp");
        while (peek().getType() == TokenType.AND) {
            consume();
            parseEqExp();
            printNonTerminal("LAndExp");
        }
    }

    // LOrExp -> LAndExp { '||' LAndExp }
    private void parseLOrExp() {
        parseLAndExp();
        printNonTerminal("LOrExp");
        while (peek().getType() == TokenType.OR) {
            consume();
            parseLAndExp();
            printNonTerminal("LOrExp");
        }
    }

    // -- 顶层表达式规则 --

    private void parseExp() {
        parseAddExp();
        printNonTerminal("Exp");
    }

    private void parseCond() {
        parseLOrExp();
        printNonTerminal("Cond");
    }

    private void parseConstExp() {
        parseAddExp();
        printNonTerminal("ConstExp");
    }


}