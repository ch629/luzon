package com.luzon.recursiveDescent

import com.luzon.lexer.Token
import com.luzon.lexer.Token.Literal
import com.luzon.lexer.Token.Symbol.*

internal class ExpressionParser(private val rd: RecursiveDescent) {

    fun expression() = firstNotNullOrNull(unaryExpr(), literal())

    private fun unaryExpr() = unarySub()

    private fun unarySub(): Expression? {
        return acceptExpr(SUBTRACT, ::expression)
    }

    private fun literal(): Expression? {
        val token = accept(Literal.INT)
        var literal: Expression? = null

        if (token != null) {
            literal = Expression.Literal.IntLiteral(token.data.toInt())
            val binary = binaryExpr(literal)
            if (binary != null) return binary
        }

        return literal
    }

    private fun binaryExpr(lhs: Expression) = firstNotNullOrNull(plusExpr(lhs), subExpr(lhs), multExpr(lhs), divExpr(lhs))

    private fun plusExpr(lhs: Expression) = acceptBinary(PLUS, Expression.BinaryExpr::PlusExpr, lhs)
    private fun subExpr(lhs: Expression) = acceptBinary(SUBTRACT, Expression.BinaryExpr::SubExpr, lhs)
    private fun multExpr(lhs: Expression) = acceptBinary(MULTIPLY, Expression.BinaryExpr::MultExpr, lhs)
    private fun divExpr(lhs: Expression) = acceptBinary(DIVIDE, Expression.BinaryExpr::DivExpr, lhs)

    private fun acceptBinary(tokenEnum: Token.TokenEnum, constructor: (Expression, Expression) -> Expression, lhs: Expression): Expression? {
        if (expect(tokenEnum)) {
            val rhs = expression()
            if (rhs != null) return constructor(lhs, rhs)
        }

        return null
    }

    private fun accept(enum: Token.TokenEnum) = rd.accept(enum)
    private fun matches(enum: Token.TokenEnum) = rd.expect(enum)
    private fun expect(enum: Token.TokenEnum) = accept(enum) != null

    private fun acceptExpr(enum: Token.TokenEnum, expr: () -> Expression?) = if (matches(enum)) expr() else null

    private fun <T> firstNotNullOrNull(vararg values: T?) = values.firstOrNull { it != null }
}

sealed class Expression {
    sealed class BinaryExpr(val left: Expression, val right: Expression) : Expression() {
        class PlusExpr(left: Expression, right: Expression) : BinaryExpr(left, right)
        class SubExpr(left: Expression, right: Expression) : BinaryExpr(left, right)
        class MultExpr(left: Expression, right: Expression) : BinaryExpr(left, right)
        class DivExpr(left: Expression, right: Expression) : BinaryExpr(left, right)
    }

    sealed class UnaryExpr(val expr: Expression) : Expression() {
        class SubExpr(expr: Expression) : UnaryExpr(expr)
    }

    sealed class Literal<T>(val value: T) : Expression() {
        class IntLiteral(value: Int) : Literal<Int>(value)
        class FloatLiteral(value: Float) : Literal<Float>(value)
        class DoubleLiteral(value: Double) : Literal<Double>(value)
    }
}