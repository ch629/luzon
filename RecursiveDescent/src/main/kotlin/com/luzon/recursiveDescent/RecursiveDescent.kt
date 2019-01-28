package com.luzon.recursiveDescent

import com.luzon.lexer.Token
import com.luzon.lexer.TokenStream
import com.luzon.utils.Predicate

fun parse(tokens: TokenStream) {
    TODO()
}

internal class RecursiveDescent(tokens: TokenStream) {
    private val iterator = tokens.iterator()
    var token: Token? = null

    init {
        next()
    }

    fun next(): Boolean {
        token = if (iterator.hasNext()) iterator.next() else null
        return token != null
    }

    fun expect(pred: Predicate<Token.TokenEnum>) = token != null && pred(token!!.tokenEnum)
    fun expect(tokenEnum: Token.TokenEnum) = expect { it == tokenEnum }
    fun expect(vararg tokenEnums: Token.TokenEnum) = token != null && tokenEnums.any { it == token!!.tokenEnum }

    fun accept(pred: Predicate<Token.TokenEnum>): Token? {
        val rightType = expect(pred)
        val token = token

        return if (rightType) {
            next()
            token
        } else null
    }

    fun accept(tokenEnum: Token.TokenEnum) = accept { it == tokenEnum }

    fun consume(tokenEnum: Token.TokenEnum) = accept(tokenEnum)
    fun consume(pred: Predicate<Token.TokenEnum>) = accept(pred)

    fun matches(tokenEnum: Token.TokenEnum) = expect(tokenEnum)
    fun matches(pred: Predicate<Token.TokenEnum>) = expect(pred)

    fun matchConsume(tokenEnum: Token.TokenEnum) = accept(tokenEnum) != null

    fun matches(tokenEnum: Token.TokenEnum, func: (Token) -> Boolean) = if (matches(tokenEnum)) func(token!!) else false
}