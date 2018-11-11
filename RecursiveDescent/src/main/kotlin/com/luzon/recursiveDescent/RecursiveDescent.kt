package com.luzon.recursiveDescent

import com.luzon.lexer.Token

fun parse(tokens: Sequence<Token>) {
    TODO()
}

internal class RecursiveDescent(tokens: Sequence<Token>) {
    private val iterator = tokens.iterator()
    var token: Token? = null

    init {
        next()
    }

    fun next(): Boolean {
        token = if (iterator.hasNext()) iterator.next() else null
        return token != null
    }

    fun expect(tokenEnum: Token.TokenEnum): Boolean {
        return token != null && token!!.tokenEnum == tokenEnum
    }

    fun expect(vararg tokenEnums: Token.TokenEnum): Boolean {
        return token != null && tokenEnums.any { it == token!!.tokenEnum }
    }

    fun accept(tokenEnum: Token.TokenEnum): Token? {
        val rightType = expect(tokenEnum)
        val token = token

        return if (rightType) {
            next()
            token
        } else null
    }
}