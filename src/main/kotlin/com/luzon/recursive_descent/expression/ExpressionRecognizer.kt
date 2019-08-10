package com.luzon.recursive_descent.expression

import com.luzon.lexer.Token.Literal
import com.luzon.lexer.Token.Symbol.AND
import com.luzon.lexer.Token.Symbol.DECREMENT
import com.luzon.lexer.Token.Symbol.DIVIDE
import com.luzon.lexer.Token.Symbol.DOT
import com.luzon.lexer.Token.Symbol.EQUAL_EQUAL
import com.luzon.lexer.Token.Symbol.GREATER
import com.luzon.lexer.Token.Symbol.GREATER_EQUAL
import com.luzon.lexer.Token.Symbol.INCREMENT
import com.luzon.lexer.Token.Symbol.LESS
import com.luzon.lexer.Token.Symbol.LESS_EQUAL
import com.luzon.lexer.Token.Symbol.L_PAREN
import com.luzon.lexer.Token.Symbol.MOD
import com.luzon.lexer.Token.Symbol.MULTIPLY
import com.luzon.lexer.Token.Symbol.NOT
import com.luzon.lexer.Token.Symbol.NOT_EQUAL
import com.luzon.lexer.Token.Symbol.OR
import com.luzon.lexer.Token.Symbol.PLUS
import com.luzon.lexer.Token.Symbol.R_PAREN
import com.luzon.lexer.Token.Symbol.SUBTRACT
import com.luzon.lexer.TokenStream
import com.luzon.recursive_descent.TokenRDStream

@Suppress("SimplifyBooleanWithConstants")
internal class ExpressionRecognizer(private val rd: TokenRDStream) {
    private val exprList = ExpressionStreamList()

    companion object {
        val binaryOperators = listOf(PLUS, SUBTRACT, MULTIPLY, DIVIDE, MOD, LESS, LESS_EQUAL, EQUAL_EQUAL, GREATER_EQUAL, GREATER, AND, OR, NOT_EQUAL)
        val unaryOperators = listOf(SUBTRACT, NOT)

        fun recognize(rd: TokenRDStream) = ExpressionRecognizer(rd).recognize()
        fun recognize(tokens: TokenStream) = ExpressionRecognizer(TokenRDStream(tokens)).recognize()
    }

    fun recognize() = if (expression()) exprList.toStream() else null

    private fun expression() = grouping() || literal() || unaryOperator()

    private fun grouping(): Boolean {
        if (rd.matchConsume(L_PAREN)) {
            exprList += L_PAREN

            if (expression() && rd.matchConsume(R_PAREN)) {
                exprList += R_PAREN
                return binaryOperator() || true
            }
        }

        return false
    }

    private fun literal(): Boolean {
        val literal = getLiteral()

        if (literal != null) {
            exprList += dotChain(literal) ?: literal

            return binaryOperator() || true
        }

        return false
    }

    private fun getLiteral(): ExpressionToken? {
        var incDec = rd.accept(INCREMENT, DECREMENT)
        val literal = rd.accept { it.tokenEnum is Literal }

        if (literal != null) {
            if (literal.tokenEnum == Literal.IDENTIFIER) {
                if (incDec != null) {
                    return if (incDec.tokenEnum == INCREMENT) ExpressionToken.IncrementId(false, literal)
                    else ExpressionToken.DecrementId(false, literal)
                }

                if (rd.matches(L_PAREN)) {
                    val funCall = FunctionCallParser(literal.data, rd).parse()

                    if (funCall != null) return ExpressionToken.FunctionCall(funCall) // TODO: else error
                }

                incDec = rd.accept(INCREMENT, DECREMENT)
                if (incDec != null) {
                    return if (incDec.tokenEnum == INCREMENT) ExpressionToken.IncrementId(true, literal)
                    else ExpressionToken.DecrementId(true, literal)
                }
            }

            return ExpressionToken.fromToken(literal)
        }

        return null
    }

    private fun dotChain(first: ExpressionToken? = null): ExpressionToken.DotChain? {
        if (first != null && rd.matches(DOT))
            return ExpressionToken.DotChain(first, dotChain())

        if (rd.matchConsume(DOT)) {
            val literal = getLiteral()

            if (literal != null)
                return ExpressionToken.DotChain(literal, dotChain())
        }

        return null
    }

    private fun binaryOperator(): Boolean {
        val op = rd.accept { it.tokenEnum in binaryOperators }

        if (op != null) {
            exprList += op

            return expression()
        }

        return false
    }

    private fun unaryOperator(): Boolean {
        val op = rd.accept { it.tokenEnum in unaryOperators }

        if (op != null) {
            exprList.add(op, true)

            return expression()
        }

        return false
    }
}
