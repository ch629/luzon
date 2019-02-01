package com.luzon.rd

import com.luzon.lexer.Token.Literal
import com.luzon.lexer.Token.Symbol.*

internal class NewExpressionRecognizer(private val rd: RecursiveDescent) {
    private var parenIndent = 0
    private val expressionList = ExpressionStreamList()

    companion object {
        // TODO: Increment, Decrement, etc?
        private val binaryOperators = listOf(PLUS, SUBTRACT, MULTIPLY, DIVIDE, MOD, LESS, LESS_EQUAL, EQUAL_EQUAL, GREATER_EQUAL, GREATER, AND, OR, NOT_EQUAL)
        private val unaryOperators = listOf(SUBTRACT, NOT)

        fun recognize(rd: RecursiveDescent) = NewExpressionRecognizer(rd).recognize()
    }

    fun recognize() = if (expression()) expressionList.toStream() else null

    private fun expression(): Boolean = literal() || unaryOperator() || openParen()

    private fun literal(): Boolean = rd.accept({ it is Literal }, { literal ->
        fun afterLiteral() = binaryOperator() || closeParen() || true

        if (literal.tokenEnum == Literal.IDENTIFIER && rd.matches(L_PAREN)) {
            val functionCall = FunctionCallParser(literal.data, rd).parse()

            if (functionCall != null) {
                expressionList.add(functionCall)

                return@accept afterLiteral()
            }
        }

        expressionList.add(literal)
        return@accept afterLiteral()
    })

    private fun openParen(): Boolean = rd.accept(L_PAREN) {
        parenIndent++
        expressionList.add(L_PAREN)

        expression()
    }

    private fun closeParen(): Boolean = rd.accept(R_PAREN) {
        if (parenIndent > 0) {
            parenIndent--
            expressionList.add(R_PAREN)

            binaryOperator() || true
        } else false
    }

    private fun binaryOperator(): Boolean = rd.accept({ it in binaryOperators }, { operator ->
        expressionList.add(operator)

        openParen() || unaryOperator() || literal()
    })

    private fun unaryOperator(): Boolean = rd.accept({ it in unaryOperators }, { operator ->
        expressionList.add(operator, true)

        openParen() || literal()
    })
}