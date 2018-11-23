package com.luzon.recursiveDescent.ast

import com.luzon.lexer.Token
import com.luzon.recursiveDescent.FunctionCallToken
import com.luzon.recursiveDescent.parseExpression

sealed class Expression {
    sealed class Binary(val left: Expression, val right: Expression) : Expression() {
        class PlusExpr(left: Expression, right: Expression) : Binary(left, right)
        class SubExpr(left: Expression, right: Expression) : Binary(left, right)
        class MultExpr(left: Expression, right: Expression) : Binary(left, right)
        class DivExpr(left: Expression, right: Expression) : Binary(left, right)
    }

    sealed class Unary(val expr: Expression) : Expression() {
        class SubExpr(expr: Expression) : Unary(expr)
    }

    sealed class LiteralExpr : Expression() {
        class IntLiteral(val value: Int) : LiteralExpr() {
            companion object {
                fun fromToken(token: Token): IntLiteral = IntLiteral(token.data.toInt())
            }
        }

        class FloatLiteral(val value: Float) : LiteralExpr() {
            companion object {
                fun fromToken(token: Token): FloatLiteral = FloatLiteral(token.data.toFloat())
            }
        }

        class DoubleLiteral(val value: Double) : LiteralExpr() {
            companion object {
                fun fromToken(token: Token): DoubleLiteral = DoubleLiteral(token.data.toDouble())
            }
        }

        class IdentifierLiteral(val name: String) : LiteralExpr() {
            companion object {
                fun fromToken(token: Token): IdentifierLiteral = IdentifierLiteral(token.data)
            }
        }

        class FunctionCall(val name: String, val params: List<Expression>) : LiteralExpr() {
            companion object {
                internal fun fromToken(token: FunctionCallToken): FunctionCall {
                    return FunctionCall(token.id, token.params.mapNotNull { parseExpression(it) })  // TODO: Need to convert each sequence of params into an Expression
                }
            }
        }
    }
}