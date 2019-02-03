package com.luzon.rd

import com.luzon.lexer.Token
import com.luzon.lexer.Token.Symbol.*
import com.luzon.lexer.TokenStream
import com.luzon.rd.ast.Expression

fun precedenceClimb(tokens: TokenStream) = PrecedenceClimbing(TokenRDStream(tokens)).parse()
internal fun precedenceClimb(rd: TokenRDStream) = PrecedenceClimbing(rd).parse()

internal class PrecedenceClimbing(rd: TokenRDStream) {
    private val rd: ExpressionRDStream = ExpressionRDStream(ExpressionRecognizer.recognize(rd)
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
                    PLUS -> Expression.Binary::PlusExpr
                    SUBTRACT -> Expression.Binary::SubExpr
                    MULTIPLY -> Expression.Binary::MultExpr
                    DIVIDE -> Expression.Binary::DivExpr

                    EQUAL_EQUAL -> Expression.Binary::Equals
                    NOT_EQUAL -> Expression.Binary::NotEquals
                    GREATER_EQUAL -> Expression.Binary::GreaterEquals
                    GREATER -> Expression.Binary::Greater
                    LESS -> Expression.Binary::Less
                    LESS_EQUAL -> Expression.Binary::LessEquals
                    AND -> Expression.Binary::And
                    OR -> Expression.Binary::Or
                    else -> null
                }?.invoke(left, right) ?: left
            }
        } while (n != null)

        return left
    }

    fun p(): Expression? {
        if (rd.matches { it is ExpressionToken.FunctionCall }) { // TODO: When this -> Need something like a peek, then use when, and consume if it hits one?
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
        } else if (rd.matches { it is ExpressionToken.ExpressionLiteral }) {
            val literal = rd.consume() as ExpressionToken.ExpressionLiteral

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