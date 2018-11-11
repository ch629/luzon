package com.luzon.recursiveDescent

import com.luzon.lexer.Token
import com.luzon.lexer.Token.Literal
import com.luzon.lexer.Token.Symbol.*
import com.luzon.recursiveDescent.Expression.BinaryExpr
import com.luzon.recursiveDescent.Expression.Literal.*

fun parseExpression(tokens: Sequence<Token>) = ExpressionParser(RecursiveDescent(tokens)).expression()

internal class ExpressionParser(private val rd: RecursiveDescent) {

    fun expression() = firstNotNullOrNull(unaryExpr(), literal())

    private fun unaryExpr() = unarySub()

    private fun unarySub(): Expression? {
        return acceptExpr(SUBTRACT, ::expression)
    }

    private fun literal(): Expression? {
        val expression: Expression? = firstNotNullOrNull(
                literalDef(Literal.INT) { IntLiteral.fromToken(it) },
                literalDef(Literal.FLOAT) { FloatLiteral.fromToken(it) },
                literalDef(Literal.DOUBLE) { DoubleLiteral.fromToken(it) }
        )

        if (expression != null) {
            val binary = binaryExpr(expression)
            if (binary != null) return binary
        }

        return expression
    }

    private fun literalDef(tokenEnum: Token.TokenEnum, literalConstructor: (Token) -> Expression): Expression? {
        val token = accept(tokenEnum)
        var literal: Expression? = null

        if (token != null) {
            literal = literalConstructor(token)
            val binary = binaryExpr(literal)
            if (binary != null) literal = binary
        }

        return literal
    }

    private fun binaryExpr(lhs: Expression) = firstNotNullOrNull(plusExpr(lhs), subExpr(lhs), multExpr(lhs), divExpr(lhs))

    private fun plusExpr(lhs: Expression) = acceptBinary(PLUS, BinaryExpr::PlusExpr, lhs)
    private fun subExpr(lhs: Expression) = acceptBinary(SUBTRACT, BinaryExpr::SubExpr, lhs)
    private fun multExpr(lhs: Expression) = acceptBinary(MULTIPLY, BinaryExpr::MultExpr, lhs)
    private fun divExpr(lhs: Expression) = acceptBinary(DIVIDE, BinaryExpr::DivExpr, lhs)

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

    sealed class Literal : Expression() {
        class IntLiteral(val value: Int) : Literal() {
            companion object {
                fun fromToken(token: Token): IntLiteral = IntLiteral(token.data.toInt())
            }
        }

        class FloatLiteral(val value: Float) : Literal() {
            companion object {
                fun fromToken(token: Token): FloatLiteral = FloatLiteral(token.data.toFloat())
            }
        }

        class DoubleLiteral(val value: Double) : Literal() {
            companion object {
                fun fromToken(token: Token): DoubleLiteral = DoubleLiteral(token.data.toDouble())
            }
        }
    }
}