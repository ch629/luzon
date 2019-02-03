package com.luzon.rd

import com.luzon.lexer.Token
import com.luzon.utils.Predicate

internal class ExpressionRecursiveDescent(expr: ExpressionStream) {
    private val iterator = expr.iterator()
    private var token: ExpressionToken? = null

    init {
        next()
    }

    fun next(): Boolean {
        token = if (iterator.hasNext()) iterator.next() else null
        return token != null
    }

    fun expect(pred: Predicate<ExpressionToken>) = token != null && pred(token!!)
    fun expect(tokenEnum: Token.TokenEnum) = expect { it == tokenEnum }

    fun accept(pred: Predicate<ExpressionToken>): ExpressionToken? {
        val rightType = expect(pred)
        val token = token

        return if (rightType) {
            next()
            token
        } else null
    }

    fun accept(tokenEnum: Token.TokenEnum) = accept { it.tokenType == tokenEnum }

    fun consume(pred: Predicate<ExpressionToken>) = accept(pred)
    fun consume() = accept { true }

    fun matches(tokenEnum: Token.TokenEnum) = expect(tokenEnum)
    fun matches(pred: Predicate<ExpressionToken>) = expect(pred)

    fun matchConsume(tokenEnum: Token.TokenEnum) = accept(tokenEnum) != null
}