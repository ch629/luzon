package com.luzon.rd.ast

import com.luzon.lexer.Token

sealed class Expression {
    sealed class Binary(var left: Expression?, var right: Expression?) : Expression() { // TODO: Mod?
        class PlusExpr(left: Expression? = null, right: Expression? = null) : Binary(left, right) // TODO: Keep these separate, or use an Enum to distinguish between each? i.e. Binary(operator, left, right)
        class SubExpr(left: Expression? = null, right: Expression? = null) : Binary(left, right)
        class MultExpr(left: Expression? = null, right: Expression? = null) : Binary(left, right)
        class DivExpr(left: Expression? = null, right: Expression? = null) : Binary(left, right)

        class Equals(left: Expression? = null, right: Expression? = null) : Binary(left, right)
        class NotEquals(left: Expression? = null, right: Expression? = null) : Binary(left, right)
        class GreaterEquals(left: Expression? = null, right: Expression? = null) : Binary(left, right)
        class Greater(left: Expression? = null, right: Expression? = null) : Binary(left, right)
        class Less(left: Expression? = null, right: Expression? = null) : Binary(left, right)
        class LessEquals(left: Expression? = null, right: Expression? = null) : Binary(left, right)

        class And(left: Expression? = null, right: Expression? = null) : Binary(left, right)
        class Or(left: Expression? = null, right: Expression? = null) : Binary(left, right)
    }

    sealed class Unary(var expr: Expression?) : Expression() {
        class SubExpr(expr: Expression? = null) : Unary(expr)
        class NotExpr(expr: Expression? = null) : Unary(expr)
    }

    sealed class LiteralExpr : Expression() {
        companion object {
            fun fromToken(token: Token): LiteralExpr? {
                return when (token.tokenEnum) {
                    Token.Literal.INT -> IntLiteral.fromToken(token)
                    Token.Literal.FLOAT -> FloatLiteral.fromToken(token)
                    Token.Literal.DOUBLE -> DoubleLiteral.fromToken(token)
                    Token.Literal.IDENTIFIER -> IdentifierLiteral.fromToken(token)
                    else -> null
                }
            }
        }

        data class IntLiteral(val value: Int) : LiteralExpr() {
            companion object {
                fun fromToken(token: Token): IntLiteral = IntLiteral(token.data.toInt())
            }
        }

        data class FloatLiteral(val value: Float) : LiteralExpr() {
            companion object {
                fun fromToken(token: Token): FloatLiteral = FloatLiteral(token.data.toFloat())
            }
        }

        data class DoubleLiteral(val value: Double) : LiteralExpr() {
            companion object {
                fun fromToken(token: Token): DoubleLiteral = DoubleLiteral(token.data.toDouble())
            }
        }

        data class IdentifierLiteral(val name: String) : LiteralExpr() {
            companion object {
                fun fromToken(token: Token): IdentifierLiteral = IdentifierLiteral(token.data)
            }
        }

        data class FunctionCall(val name: String, val params: List<Expression>) : LiteralExpr()
    }
}