package com.luzon.lexer

import com.luzon.fsm.FSMachine
import com.luzon.utils.toMergedFSM

object TokenMachine {
    private val fsm = FSMachine.merge(
            TokenType.LITERAL.toFSM(),
            TokenType.KEYWORD.toFSM(),
            TokenType.SYMBOL.toFSM(),
            TokenType.COMMENT.toFSM()
    )

    fun getFSM() = fsm.copy() //So multiple machines can run at the same time
}

enum class TokenType {
    KEYWORD, SYMBOL, LITERAL, COMMENT;

    fun toFSM(): FSMachine<Char, TokenEnum> {
        val machine: FSMachine<Char, TokenEnum>? = when (this) {
            KEYWORD -> Keyword.values().map { it.toFSM() }.toMergedFSM()
            SYMBOL -> Symbol.values().map { it.toFSM() }.toMergedFSM()
            LITERAL -> Literal.values().map { it.toFSM() }.toMergedFSM()
            COMMENT -> Comment.values().map { it.toFSM() }.toMergedFSM()
        }

        return machine ?: FSMachine(emptyList())
    }
}

interface TokenEnum {
    fun id() = when (this) {
        is Enum<*> -> generateID()
        else -> null
    }

    fun regex() = when (this) {
        is Literal -> regex
        is Comment -> regex
        is Keyword -> replaceMetacharacters(regex ?: generateID())
        is Symbol -> replaceMetacharacters(regex)
        else -> ""
    }

    fun isId(id: String) = id() == id

    fun toToken(data: String) = Token(this, data)
    fun toToken() = toToken("")

    fun toFSM() = FSMachine.fromRegex<TokenEnum>(regex()).setOutput(this)
}

private fun replaceMetacharacters(regex: String): String {
    var newRegex = regex

    "\\*+?[]().".forEach {
        newRegex = newRegex.replace("$it", "\\$it")
    }

    return newRegex
}

object None : TokenEnum {
    override fun toString() = "none"
}

enum class Keyword(val regex: String? = null) : TokenEnum {
    FOR, WHILE, IF, ELSE, WHEN, BREAK,
    VAR, VAL, FUN, CLASS, ABSTRACT, ENUM,
    DOUBLE("Double"),
    FLOAT("Float"),
    INT("Int"),
    STRING("String"),
    CHAR("Char"),
    BOOLEAN("Boolean"),
    IS, AS, IN;

    override fun toString() = "keyword:${id()}"
}

enum class Symbol(val regex: String) : TokenEnum {
    EQUAL("="), EQUAL_EQUAL("=="), NOT("!"),
    NOT_EQUAL("!="), GREATER(">"), LESS("<"),
    GREATER_EQUAL(">="), LESS_EQUAL("<="),
    AND("&&"), OR("||"), PLUS("+"),
    SUBTRACT("-"), MULTIPLY("*"), DIVIDE("/"),
    MOD("%"), PLUS_ASSIGN("+="), SUBTRACT_ASSIGN("-="),
    MULTIPLY_ASSIGN("*="), DIVIDE_ASSIGN("/="),
    MOD_ASSIGN("%="), TYPE(":"), INCREMENT("++"),
    DECREMENT("--"), L_PAREN("("), R_PAREN(")"),
    L_BRACE("{"), R_BRACE("}"), L_BRACKET("["),
    R_BRACKET("]"), RANGE(".."), ARROW("->"),
    DOT("."); //DOT is for dot notation

    override fun toString() = "symbol:${id()}"
}

enum class Literal(val regex: String) : TokenEnum {
    DOUBLE("[1-9]\\d*d|\\d*\\.\\d+d?"),
    FLOAT("[1-9]\\d*f|\\d*\\.\\d+f"),
    INT("[1-9]\\d*"),
    STRING("\".*\""),
    CHAR("'\\?.'"),
    BOOLEAN("true|false"),
    IDENTIFIER("[A-Za-z_]\\w*");

    override fun toString() = "literal:${id()}"
}

enum class Comment(val regex: String) : TokenEnum {
    COMMENT_SINGLE("//.*\n"),
    COMMENT_MULTI("/\\*[.\n]*\\*/");

    override fun toString() = "comment:${id()}"
}

private fun Enum<*>.generateID(): String {
    val sb = StringBuffer()
    val scanner = Scanner(name.toLowerCase())

    while (!scanner.isAtEnd()) {
        var c = scanner.advance()
        if (c == '_') c = scanner.advance().toUpperCase()

        sb.append(c)
    }

    return sb.toString()
}