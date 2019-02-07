package com.luzon.rd

import com.luzon.lexer.Token
import com.luzon.lexer.Token.Symbol.*
import com.luzon.lexer.TokenStream
import com.luzon.rd.ast.Expression
import com.luzon.rd.expression.ExpressionRDStream
import com.luzon.rd.expression.ExpressionRecognizer
import com.luzon.rd.expression.ExpressionToken

fun main() {
    fun int(value: Int) = Token.Literal.INT.toToken(value.toString())
    fun id(name: String) = Token.Literal.IDENTIFIER.toToken(name)
    val plus = PLUS.toToken()
    val mult = MULTIPLY.toToken()
    val sub = SUBTRACT.toToken()
    val equals = EQUAL_EQUAL.toToken()
    val lParen = L_PAREN.toToken()
    val rParen = R_PAREN.toToken()
    val comma = COMMA.toToken()
    val and = AND.toToken()
    val or = OR.toToken()
    val greater = GREATER.toToken()
    val lesser = LESS.toToken()
    val TRUE = Token.Literal.BOOLEAN.toToken("true")
    val FALSE = Token.Literal.BOOLEAN.toToken("false")

    val a = parseExpression(sequenceOf(int(2), plus, int(5)))

    // Numerical
    val b = parseExpression(sequenceOf(id("apple"), plus, int(2)))

    val c = parseExpression(sequenceOf(id("func"), lParen, int(1), plus, int(2), comma, int(3), plus, int(4), rParen, plus, int(5)))

    val expr = ExpressionRecognizer.recognize(sequenceOf(lParen, int(1), plus, int(2), rParen))

    // Boolean
    val d = parseExpression(sequenceOf(lParen, int(5), plus, int(2), rParen))
    val e = parseExpression(sequenceOf(FALSE, or, TRUE))
    val f = parseExpression(sequenceOf(int(2), greater, int(5)))
    val seq = sequenceOf(
            id("a"), mult, id("b"), sub, id("c"), mult, id("d"), sub, id("e"), mult,
            id("f"), equals, id("g"), mult, id("h"), sub, id("i"), mult, id("j"),
            sub, id("k"), mult, id("l"))

    val g = parseExpression(seq)

    val i = 5
}

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
                SUBTRACT -> Expression.Unary::SubExpr
                NOT -> Expression.Unary::NotExpr
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
        }

        return null
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