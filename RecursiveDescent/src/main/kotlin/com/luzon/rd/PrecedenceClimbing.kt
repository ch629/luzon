package com.luzon.rd

import com.luzon.lexer.Token
import com.luzon.lexer.Token.Symbol.*
import com.luzon.lexer.TokenStream
import com.luzon.rd.ast.Expression

fun precedenceClimb(tokens: TokenStream) = PrecedenceClimbing(RecursiveDescent(tokens)).parse()
internal fun precedenceClimb(rd: RecursiveDescent) = PrecedenceClimbing(rd).parse()

internal class PrecedenceClimbing(rd: RecursiveDescent) {
    private val rd: ExpressionRecursiveDescent = ExpressionRecursiveDescent(ExpressionRecognizer.recognize(rd)
            ?: emptySequence())

    fun parse() = exp(0)

    fun exp(prec: Int): Expression? {
        var left = p()

        do {
            val n = rd.consume { it is ExpressionToken.BinaryOperator && it.precedence(false) >= prec }

            if (n != null) {
                val q = n.precedence() + if (n.leftAssociative()) 1 else 0
                val right = exp(q)

                left = when (n.tokenType) {
                    PLUS -> Expression.Binary.PlusExpr(left, right)
                    SUBTRACT -> Expression.Binary.SubExpr(left, right)
                    MULTIPLY -> Expression.Binary.MultExpr(left, right)
                    DIVIDE -> Expression.Binary.DivExpr(left, right)

                    EQUAL_EQUAL -> Expression.Binary.Equals(left, right)
                    NOT_EQUAL -> Expression.Binary.NotEquals(left, right)
                    GREATER_EQUAL -> Expression.Binary.GreaterEquals(left, right)
                    GREATER -> Expression.Binary.Greater(left, right)
                    LESS -> Expression.Binary.Less(left, right)
                    LESS_EQUAL -> Expression.Binary.LessEquals(left, right)
                    AND -> Expression.Binary.And(left, right)
                    OR -> Expression.Binary.Or(left, right)
                    else -> left // TODO: null?
                }
            }
        } while (n != null)

        return left
    }

    fun p(): Expression? {
        if (rd.matches { it is ExpressionToken.FunctionCall }) {
            return (rd.consume() as ExpressionToken.FunctionCall).function
        } else if (rd.matches { it is ExpressionToken.UnaryOperator }) {
            val unary = rd.consume()

            val q = unary!!.precedence(true)
            val expr = exp(q)

            return when (unary.tokenType) {
                SUBTRACT -> Expression.Unary.SubExpr(expr)
                NOT -> Expression.Unary.NotExpr(expr)
                else -> null
            }
        } else if (rd.matchConsume(L_PAREN)) {
            val t = exp(0)

            if (rd.matchConsume(R_PAREN)) {
                return t
            } // else error
        } else if (rd.matches { it.tokenType is Token.Literal }) {
            val literal = rd.consume()

            if (literal is ExpressionToken.ExpressionLiteral)
                return Expression.LiteralExpr.fromToken(literal.token) // TODO: Error if null?
        }

        return null
    }

    private fun ExpressionToken.precedence(unary: Boolean = false) = tokenType?.precedence(unary) ?: -1

    private fun Token.TokenEnum.precedence(unary: Boolean = false) = when (this) {
        OR -> 0
        AND -> 1
        EQUAL_EQUAL, LESS_EQUAL, LESS, GREATER, GREATER_EQUAL, NOT_EQUAL -> 2 // TODO: Check these.
        PLUS, SUBTRACT -> if (unary) 4 else 3
        MULTIPLY, DIVIDE -> 5// TODO: Mod?
        else -> -1
    }

    private fun ExpressionToken.leftAssociative() = tokenType?.leftAssociative() ?: false

    private fun Token.TokenEnum.leftAssociative() = when (this) {
        OR, AND, EQUAL_EQUAL, PLUS, SUBTRACT, MULTIPLY, DIVIDE -> true // TODO: Mod?
        else -> false
    }
}