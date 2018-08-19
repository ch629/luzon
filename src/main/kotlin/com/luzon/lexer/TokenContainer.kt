package com.luzon.lexer

import com.luzon.lexer.TokenType.*

fun findToken(id: String, type: TokenType) = when (type) {
    KEYWORD -> Keyword.values().firstOrNull { it.id == id }
    SYMBOL -> Symbol.values().firstOrNull { it.id == id }
    LITERAL -> Literal.values().firstOrNull { it.id == id }
    COMMENT -> Comment.values().firstOrNull { it.id == id }
}

sealed class TokenContainer
data class KeywordContainer(val keyword: Keyword) : TokenContainer()
data class SymbolContainer(val symbol: Symbol) : TokenContainer()
data class LiteralContainer(val literal: Literal) : TokenContainer()
object None : TokenContainer()

data class TokenHolder(val container: TokenContainer)

enum class TokenType {
    KEYWORD, SYMBOL, LITERAL, COMMENT
}

enum class Keyword {
    FOR, WHILE, IF, ELSE, WHEN, BREAK,
    VAR, VAL, FUN, CLASS, ABSTRACT, ENUM,
    DOUBLE, FLOAT, INT, STRING, CHAR, BOOLEAN,
    IS, AS;

    val id: String by lazy { name.toLowerCase() }
}

enum class Symbol {
    EQUAL, EQUAL_EQUAL, NOT, NOT_EQUAL, GREATER, LESS,
    GREATER_EQUAL, LESS_EQUAL, AND, OR, PLUS, SUBTRACT,
    MULTIPLY, DIVIDE, MOD, PLUS_ASSIGN, SUBTRACT_ASSIGN, MULTIPLY_ASSIGN,
    DIVIDE_ASSIGN, MOD_ASSIGN, TYPE, INCREMENT, DECREMENT, L_PAREN,
    R_PAREN, L_BRACE, R_BRACE, L_BRACKET, R_BRACKET, RANGE,
    ARROW, DOT; //DOT is for dot notation

    val id: String by lazy { generateID() }
}

enum class Literal {
    DOUBLE, FLOAT, INT, STRING, CHAR, BOOLEAN,
    IDENTIFIER; //Try to find somewhere for IDENTIFIER? It works similarly to a literal, but it's not really one

    val id: String by lazy { name.toLowerCase() }
}

enum class Comment {
    COMMENT_SINGLE, COMMENT_MULTI;

    val id: String by lazy { generateID() }
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