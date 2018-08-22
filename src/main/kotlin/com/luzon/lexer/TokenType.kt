package com.luzon.lexer

enum class TokenType {
    KEYWORD, SYMBOL, LITERAL, COMMENT;

    fun findToken(id: String): TokenEnum {
        val enum: TokenEnum? = when (this) {
            KEYWORD -> Keyword.values().firstOrNull { it.isId(id) }
            SYMBOL -> Symbol.values().firstOrNull { it.isId(id) }
            LITERAL -> Literal.values().firstOrNull { it.isId(id) }
            COMMENT -> Comment.values().firstOrNull { it.isId(id) }
        }

        return enum ?: None
    }
}

interface TokenEnum {
    fun id() = when (this) {
        is Enum<*> -> generateID()
        else -> null
    }

    fun isId(id: String) = id() == id

    fun toToken(data: String) = Token(this, data)
    fun toToken() = toToken("")
}

object None : TokenEnum {
    override fun toString() = "none"
}

enum class Keyword : TokenEnum {
    FOR, WHILE, IF, ELSE, WHEN, BREAK,
    VAR, VAL, FUN, CLASS, ABSTRACT, ENUM,
    DOUBLE, FLOAT, INT, STRING, CHAR, BOOLEAN,
    IS, AS, IN;

    override fun toString() = "keyword:${id()}"
}

enum class Symbol : TokenEnum {
    EQUAL, EQUAL_EQUAL, NOT, NOT_EQUAL, GREATER, LESS,
    GREATER_EQUAL, LESS_EQUAL, AND, OR, PLUS, SUBTRACT,
    MULTIPLY, DIVIDE, MOD, PLUS_ASSIGN, SUBTRACT_ASSIGN, MULTIPLY_ASSIGN,
    DIVIDE_ASSIGN, MOD_ASSIGN, TYPE, INCREMENT, DECREMENT, L_PAREN,
    R_PAREN, L_BRACE, R_BRACE, L_BRACKET, R_BRACKET, RANGE,
    ARROW, DOT; //DOT is for dot notation

    override fun toString() = "symbol:${id()}"
}

enum class Literal : TokenEnum {
    DOUBLE, FLOAT, INT, STRING, CHAR, BOOLEAN,
    IDENTIFIER;

    override fun toString() = "literal:${id()}"
}

enum class Comment : TokenEnum {
    COMMENT_SINGLE, COMMENT_MULTI;

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