package com.luzon.lexer

//TODO: Create token from Container (Still need to implement actual tokens; probably best to delete all my old token code for that, as these tokens will just hold their value along with the type, rather than being used to create an AST)
sealed class TokenContainer(val tokenType: Enum<*>? = null, private val prefix: String? = null) {
    override fun toString() = "${prefix!!}:${tokenType!!.name}"
}

class KeywordContainer(keyword: Keyword) : TokenContainer(keyword, "keyword")
class SymbolContainer(symbol: Symbol) : TokenContainer(symbol, "symbol")
class LiteralContainer(literal: Literal) : TokenContainer(literal, "literal")
class CommentContainer(comment: Comment) : TokenContainer(comment, "comment")

object None : TokenContainer() {
    override fun toString() = "none"
}

data class TokenHolder(val container: TokenContainer) {
    override fun toString() = container.toString()
}

enum class TokenType {
    KEYWORD, SYMBOL, LITERAL, COMMENT;

    fun findToken(id: String): TokenEnum? = when (this) {
        KEYWORD -> Keyword.values().firstOrNull { it.isId(id) }
        SYMBOL -> Symbol.values().firstOrNull { it.isId(id) }
        LITERAL -> Literal.values().firstOrNull { it.isId(id) }
        COMMENT -> Comment.values().firstOrNull { it.isId(id) }
    }
}

interface TokenEnum {
    fun toContainer() = when (this) {
        is Keyword -> KeywordContainer(this)
        is Symbol -> SymbolContainer(this)
        is Literal -> LiteralContainer(this)
        is Comment -> CommentContainer(this)
        else -> None
    }

    fun id() = when (this) {
        is Enum<*> -> generateID()
        else -> null
    }

    fun isId(id: String) = id() == id
}

enum class Keyword : TokenEnum {
    FOR, WHILE, IF, ELSE, WHEN, BREAK,
    VAR, VAL, FUN, CLASS, ABSTRACT, ENUM,
    DOUBLE, FLOAT, INT, STRING, CHAR, BOOLEAN,
    IS, AS, IN;
}

enum class Symbol : TokenEnum {
    EQUAL, EQUAL_EQUAL, NOT, NOT_EQUAL, GREATER, LESS,
    GREATER_EQUAL, LESS_EQUAL, AND, OR, PLUS, SUBTRACT,
    MULTIPLY, DIVIDE, MOD, PLUS_ASSIGN, SUBTRACT_ASSIGN, MULTIPLY_ASSIGN,
    DIVIDE_ASSIGN, MOD_ASSIGN, TYPE, INCREMENT, DECREMENT, L_PAREN,
    R_PAREN, L_BRACE, R_BRACE, L_BRACKET, R_BRACKET, RANGE,
    ARROW, DOT; //DOT is for dot notation
}

enum class Literal : TokenEnum {
    DOUBLE, FLOAT, INT, STRING, CHAR, BOOLEAN,
    IDENTIFIER; //Try to find somewhere for IDENTIFIER? It works similarly to a literal, but it's not really one
}

enum class Comment : TokenEnum {
    COMMENT_SINGLE, COMMENT_MULTI;
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