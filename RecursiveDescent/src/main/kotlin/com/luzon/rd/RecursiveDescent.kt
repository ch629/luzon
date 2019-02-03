package com.luzon.rd

import com.luzon.lexer.Token
import com.luzon.lexer.TokenStream
import com.luzon.utils.Predicate

internal class RecursiveDescent(tokens: TokenStream) {
    private val iterator = tokens.iterator()
    private var token: Token? = null

    init {
        next()
    }

    fun next(): Boolean {
        token = if (iterator.hasNext()) iterator.next() else null
        return token != null
    }

    fun expect(pred: Predicate<Token.TokenEnum>) = token != null && pred(token!!.tokenEnum)
    fun expect(tokenEnum: Token.TokenEnum) = expect { it == tokenEnum }

    fun accept(pred: Predicate<Token.TokenEnum>): Token? {
        val rightType = expect(pred)
        val token = token

        return if (rightType) {
            next()
            token
        } else null
    }

    fun accept(pred: Predicate<Token.TokenEnum>, block: (Token) -> Boolean): Boolean {
        val token = accept(pred)

        if (token != null) return block(token)
        return false
    }

    fun accept(tokenEnum: Token.TokenEnum, block: (Token) -> Boolean) = accept({ it == tokenEnum }, block)

    fun accept(tokenEnum: Token.TokenEnum) = accept { it == tokenEnum }

    fun consume() = accept { true }

    fun matches(tokenEnum: Token.TokenEnum) = expect(tokenEnum)

    fun matchConsume(tokenEnum: Token.TokenEnum) = accept(tokenEnum) != null

    fun matches(tokenEnum: Token.TokenEnum, func: (Token) -> Boolean) = if (matches(tokenEnum)) func(token!!) else false
}