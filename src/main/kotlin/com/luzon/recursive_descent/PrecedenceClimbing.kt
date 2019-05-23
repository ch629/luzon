package com.luzon.recursive_descent

import com.luzon.lexer.Token.Symbol.*
import com.luzon.lexer.TokenStream
import com.luzon.recursive_descent.ast.ASTNode.Expression
import com.luzon.recursive_descent.expression.ExpressionRDStream
import com.luzon.recursive_descent.expression.ExpressionRecognizer
import com.luzon.recursive_descent.expression.ExpressionToken

fun parseExpression(tokens: TokenStream) = PrecedenceClimbing(TokenRDStream(tokens)).parse()

internal class PrecedenceClimbing(rd: TokenRDStream) {
    private val rd: ExpressionRDStream = ExpressionRDStream(ExpressionRecognizer.recognize(rd)
            ?: emptySequence())

    fun parse() = exp(0)

    private fun exp(prec: Int): Expression? {
        var left = p()

        do {
            val n = rd.consume { it is ExpressionToken.BinaryOperator && it.precedence(false) >= prec }

            if (n != null) {
                val q = n.precedence() + if (n.leftAssociative()) 1 else 0
                val right = exp(q)

                left = Expression.Binary.fromOperator(n.tokenType, left, right) ?: left
            }
        } while (n != null)

        return left
    }

    private fun p(): Expression? {
        if (rd.matches { it is ExpressionToken.FunctionCall }) {
            return (rd.consume() as ExpressionToken.FunctionCall).function
        } else if (rd.matches { it is ExpressionToken.UnaryOperator }) {
            val unary = rd.consume()

            val q = unary!!.precedence(true)
            val expr = exp(q)

            return when (unary.tokenType) {
                SUBTRACT -> Expression.Unary::Sub
                NOT -> Expression.Unary::Not
                else -> null
            }?.invoke(expr)
        } else if (rd.matchConsume(L_PAREN)) {
            val t = exp(0)

            if (rd.matchConsume(R_PAREN)) {
                return t
            } // else error
        } else if (rd.matches { it is ExpressionToken.ExpressionLiteral }) {
            val literal = rd.consume() as ExpressionToken.ExpressionLiteral

            return Expression.LiteralExpr.fromToken(literal.token) // TODO: Error if null?
        } else if (rd.matches { it is ExpressionToken.DotChain }) {
            return dotChain(rd.consume() as ExpressionToken.DotChain)
        } else if (rd.matches { it is ExpressionToken.DecrementId }) {
            val dec = rd.consume() as ExpressionToken.DecrementId

            return Expression.Unary.Decrement(Expression.LiteralExpr.fromToken(dec.token), !dec.post)
        } else if (rd.matches { it is ExpressionToken.IncrementId }) {
            val inc = rd.consume() as ExpressionToken.IncrementId

            return Expression.Unary.Increment(Expression.LiteralExpr.fromToken(inc.token), !inc.post)
        }

        return null
    }

    private fun dotChain(first: ExpressionToken.DotChain? = null): Expression.LiteralExpr.DotChainLiteral? {
        return if (first != null)
            Expression.LiteralExpr.DotChainLiteral(
                    if (first.token is ExpressionToken.ExpressionLiteral) Expression.LiteralExpr.fromToken(first.token.token)!!
                    else (first.token as ExpressionToken.FunctionCall).function,
                    dotChain(first.next))
        else null
    }

    private fun ExpressionToken.precedence(unary: Boolean = false) = when (tokenType) {
        OR -> 0
        AND -> 1
        EQUAL_EQUAL, LESS_EQUAL, LESS, GREATER, GREATER_EQUAL, NOT_EQUAL -> 2 // TODO: Check these.
        PLUS, SUBTRACT -> if (unary) 4 else 3
        MULTIPLY, DIVIDE -> 5// TODO: Mod?
        else -> -1
    }

    // TODO: Only power is left associative -> Which I don't implement? -> If needed, just add when(tokenType)
    private fun ExpressionToken.leftAssociative() = true
}