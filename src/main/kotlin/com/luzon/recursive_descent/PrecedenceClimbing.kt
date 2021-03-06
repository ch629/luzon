package com.luzon.recursive_descent

import com.luzon.exceptions.InvalidPrecedenceTypeException
import com.luzon.lexer.Token.Symbol.AND
import com.luzon.lexer.Token.Symbol.DIVIDE
import com.luzon.lexer.Token.Symbol.EQUAL_EQUAL
import com.luzon.lexer.Token.Symbol.GREATER
import com.luzon.lexer.Token.Symbol.GREATER_EQUAL
import com.luzon.lexer.Token.Symbol.LESS
import com.luzon.lexer.Token.Symbol.LESS_EQUAL
import com.luzon.lexer.Token.Symbol.L_PAREN
import com.luzon.lexer.Token.Symbol.MODULUS
import com.luzon.lexer.Token.Symbol.MULTIPLY
import com.luzon.lexer.Token.Symbol.NOT
import com.luzon.lexer.Token.Symbol.NOT_EQUAL
import com.luzon.lexer.Token.Symbol.OR
import com.luzon.lexer.Token.Symbol.PLUS
import com.luzon.lexer.Token.Symbol.R_PAREN
import com.luzon.lexer.Token.Symbol.SUBTRACT
import com.luzon.lexer.TokenStream
import com.luzon.recursive_descent.ast.SyntaxTreeNode.Expression
import com.luzon.recursive_descent.expression.ExpressionRecognizer
import com.luzon.recursive_descent.expression.ExpressionRecursiveDescentStream
import com.luzon.recursive_descent.expression.ExpressionToken

fun parseExpression(tokens: TokenStream) = PrecedenceClimbing(TokenRecursiveDescentStream(tokens)).parse()

internal class PrecedenceClimbing(rd: TokenRecursiveDescentStream) {
    private val rd: ExpressionRecursiveDescentStream = ExpressionRecursiveDescentStream(ExpressionRecognizer.recognize(rd)
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
                SUBTRACT -> Expression.Unary::Minus
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
        EQUAL_EQUAL, LESS_EQUAL, LESS, GREATER, GREATER_EQUAL, NOT_EQUAL -> 2
        PLUS, SUBTRACT -> if (unary) 4 else 3
        MULTIPLY, DIVIDE, MODULUS -> 5
        else -> throw InvalidPrecedenceTypeException(tokenType)
    }

    // TODO: Only power is left associative -> Which I don't implement? -> If needed, just add when(tokenType)
    private fun ExpressionToken.leftAssociative() = true
}
