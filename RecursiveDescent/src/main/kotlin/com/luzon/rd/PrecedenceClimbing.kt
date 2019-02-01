package com.luzon.rd

import com.luzon.lexer.Token
import com.luzon.lexer.Token.Symbol.*
import com.luzon.rd.ast.Expression

// This will probably have to be run after going through an expression recognizer.
internal class PrecedenceClimbing(val rd: RecursiveDescent) {
    fun parse(): Expression {
        val expr = exp(0)
        // expect end?
        return expr
    }

    fun exp(prec: Int): Expression {
        val left = p()

        do {
            val n = rd.consume { isBinary(it) && it.precedence(false) >= prec }

            if (n != null) {
                val q = n.precedence() + if (n.leftAssociative()) 1 else 0
                val right = exp(q)

                // TODO: Make Expression Node from Operator, left and right

                // TODO: Maybe this is better? -> Can remove the peek stuff if so
            }
        } while (n != null)

        val next = rd.peek()

        if (next != null) {
            while (isBinary(next) && next.precedence() >= prec) { // TODO: Maybe redo the peek to just match isBinary and precedence correctly?
                rd.consume()
                val q = next.precedence() + if (next.leftAssociative()) 1 else 0

                val right = exp(q)
                // TODO: Make Expression Node from Operator, left and right
            }
        }
        TODO()
    }

    fun p(): Expression {
        val next = rd.peek()

        if (isUnary(next)) {
            rd.consume()

            val q = next!!.precedence(true)
            val expr = exp(q)

            // TODO: Make Node
        } else if (rd.matchConsume(L_PAREN)) {
            val t = exp(0)

            if (rd.matchConsume(R_PAREN)) { // expect R_PAREN
                return t
            } // else error
        } else if (rd.matches { it is Token.Literal }) {
            val literal = rd.consume()

            return Expression.LiteralExpr.fromToken(literal!!)!!
        }

        TODO()
    }

    private fun isBinary(token: Token.TokenEnum): Boolean { // TODO: Need to detect if the next token is a binary operation and not a unary one.
        TODO()
    }

    private fun isBinary(token: Token) = isBinary(token.tokenEnum)

    private fun isUnary(token: Token?): Boolean {
        TODO()
    }

    private fun binary(token: Token): Expression {
        TODO()
    }

    private fun Token.precedence(unary: Boolean = false) = tokenEnum.precedence(unary)

    private fun Token.TokenEnum.precedence(unary: Boolean = false) = when (this) {
        OR -> 0
        AND -> 1
        EQUAL_EQUAL -> 2
        PLUS, SUBTRACT -> if (unary) 4 else 3
        MULTIPLY, DIVIDE -> 5// TODO: Mod?
        else -> -1
    }

    private fun Token.leftAssociative() = tokenEnum.leftAssociative()

    private fun Token.TokenEnum.leftAssociative() = when (this) {
        OR, AND, EQUAL_EQUAL, PLUS, SUBTRACT, MULTIPLY, DIVIDE -> true // TODO: Mod?
        else -> false
    }
}