package lexer;

public enum TokenType {
    // 关键字
    CONSTTK, INTTK, STATICTK, BREAKTK, CONTINUETK, IFTK, ELSETK,
    FORTK, RETURNTK, VOIDTK, MAINTK, PRINTFTK,

    // 标识符和字面量
    IDENFR, INTCON, STRCON,

    // 运算符和分隔符
    PLUS, MINU, MULT, DIV, MOD,         // + - * / %
    LSS, LEQ, GRE, GEQ, EQL, NEQ,       // < <= > >= == !=
    ASSIGN,                             // =
    AND, OR, NOT,                       // && || !
    SEMICN, COMMA,                      // ; ,
    LPARENT, RPARENT,                   // ( )
    LBRACK, RBRACK,                     // [ ]
    LBRACE, RBRACE                      // { }
}