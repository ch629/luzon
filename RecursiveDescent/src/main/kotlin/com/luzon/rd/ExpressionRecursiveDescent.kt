package com.luzon.rd

import com.luzon.lexer.Token
import com.luzon.utils.Predicate

internal class ExpressionRecursiveDescent(expr: ExpressionStream) {
    private val iterator = expr.iterator()
    private var lookahead: ExpressionToken? = null
    var token: ExpressionToken? = null

    init {
        next()
    }

    fun next(): Boolean {
        // Init
        if (token == null && lookahead == null && iterator.hasNext())
            lookahead = iterator.next()

        token = lookahead
        lookahead = if (iterator.hasNext()) iterator.next() else null

        return token != null

//        token = if (iterator.hasNext()) iterator.next() else null
//        return token != null
    }

    fun expect(pred: Predicate<ExpressionToken>) = token != null && pred(token!!)
    fun expect(tokenEnum: Token.TokenEnum) = expect { it == tokenEnum }
    fun expect(vararg tokenEnums: Token.TokenEnum) = token != null && token!!.tokenType != null && tokenEnums.any { it == token!!.tokenType!! }

    fun accept(pred: Predicate<ExpressionToken>): ExpressionToken? {
        val rightType = expect(pred)
        val token = token

        return if (rightType) {
            next()
            token
        } else null
    }

    fun accept(pred: Predicate<ExpressionToken>, block: (ExpressionToken) -> Boolean): Boolean {
        val token = accept(pred)

        if (token != null) return block(token)
        return false
    }

    fun accept(tokenEnum: Token.TokenEnum, block: (ExpressionToken) -> Boolean) = accept({ it == tokenEnum }, block)

    fun accept(tokenEnum: Token.TokenEnum) = accept { it == tokenEnum }

    fun consume(tokenEnum: Token.TokenEnum) = accept(tokenEnum)
    fun consume(pred: Predicate<ExpressionToken>) = accept(pred)
    fun consume() = accept { true }

    fun peek() = lookahead

    fun matches(tokenEnum: Token.TokenEnum) = expect(tokenEnum)
    fun matches(pred: Predicate<ExpressionToken>) = expect(pred)

    fun matchConsume(tokenEnum: Token.TokenEnum) = accept(tokenEnum) != null

    fun matches(tokenEnum: Token.TokenEnum, func: (ExpressionToken) -> Boolean) = if (matches(tokenEnum)) func(token!!) else false
}