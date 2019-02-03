package com.luzon.rd

import com.luzon.lexer.Token.Literal
import com.luzon.lexer.Token.Symbol.*
import com.luzon.lexer.TokenStream
import com.luzon.rd.ast.Expression

internal class ExpressionRecognizer(private val rd: RecursiveDescent) {
    private var parenIndent = 0
    private val expressionList = ExpressionStreamList()

    companion object {
        // TODO: Increment, Decrement, etc?
        val binaryOperators = listOf(PLUS, SUBTRACT, MULTIPLY, DIVIDE, MOD, LESS, LESS_EQUAL, EQUAL_EQUAL, GREATER_EQUAL, GREATER, AND, OR, NOT_EQUAL)
        val unaryOperators = listOf(SUBTRACT, NOT)

        fun recognize(rd: RecursiveDescent) = ExpressionRecognizer(rd).recognize()
        fun recognize(tokens: TokenStream) = ExpressionRecognizer(RecursiveDescent(tokens)).recognize()
    }

    fun recognize() = if (expression()) expressionList.toStream() else null

    private fun expression(): Boolean = literal() || unaryOperator() || openParen()

    private fun literal(): Boolean = rd.accept({ it is Literal }, { literal ->
        var functionCall: Expression.LiteralExpr.FunctionCall? = null

        if (literal.tokenEnum == Literal.IDENTIFIER && rd.matches(L_PAREN))
            functionCall = FunctionCallParser(literal.data, rd).parse()

        if (functionCall != null) expressionList.add(functionCall)
        else expressionList.add(literal)

        binaryOperator() || closeParen() || true
    })

    // TODO: Maybe redesign this work with similarly to the precedence climbing. i.e. expect an expression, then a closeParen within openParen, rather than storing the indentation?
    private fun openParen(): Boolean = rd.accept(L_PAREN) {
        parenIndent++
        expressionList.add(L_PAREN)

        expression()
    }

    private fun closeParen(): Boolean = rd.matches(R_PAREN) {
        if (parenIndent > 0) {
            rd.consume()
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