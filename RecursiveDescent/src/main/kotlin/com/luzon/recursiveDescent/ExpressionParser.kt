package com.luzon.recursiveDescent

import com.luzon.lexer.Token
import com.luzon.lexer.Token.Literal
import com.luzon.lexer.Token.Symbol.*
import com.luzon.recursiveDescent.ast.Expression
import com.luzon.recursiveDescent.ast.Expression.LiteralExpr.*
import com.luzon.utils.popOrNull
import java.util.*

fun parseExpression(tokens: Sequence<Token>) =
        RPNExpressionParser(
                ShuntingYard
                        .fromTokenSequence(
                                ExpressionRecognizer(RecursiveDescent(tokens)).getExpressionTokens()
                        ).getOutput().asSequence())
                .parse()

internal class RPNExpressionParser(private val tokens: Sequence<Token>) {
    private val stack = Stack<Expression>()

    companion object {
        private val binaryOperators = listOf(PLUS, SUBTRACT, MULTIPLY, DIVIDE)
    }

    private fun Token.toExpression() = when (tokenEnum) {
        is Token.Literal -> when (tokenEnum) {
            Literal.INT -> IntLiteral.Companion::fromToken
            Literal.FLOAT -> FloatLiteral.Companion::fromToken
            Literal.DOUBLE -> DoubleLiteral.Companion::fromToken
            Literal.IDENTIFIER -> IdentifierLiteral.Companion::fromToken
            else -> null
        }?.invoke(this)

        in binaryOperators -> {
            val op2 = stack.pop()
            val op1 = stack.pop()

            when (tokenEnum) {
                PLUS -> Expression.Binary::PlusExpr
                SUBTRACT -> Expression.Binary::SubExpr
                MULTIPLY -> Expression.Binary::MultExpr
                DIVIDE -> Expression.Binary::DivExpr
                else -> null
            }?.invoke(op1, op2)
        }
        else -> null
    }

    fun parse(): Expression? {
        tokens.forEach {
            stack.push(it.toExpression())
        }

        return stack.popOrNull()
    }
}

internal class ExpressionRecognizer(private val rd: RecursiveDescent) {
    private val tokens = mutableListOf<Token>()
    private var parenIndent = 0

    companion object {
        private val validBinaryOperators = listOf(PLUS, SUBTRACT, MULTIPLY, DIVIDE)
    }

    fun getExpressionTokens() = if (expression()) tokens.asSequence() else emptySequence()

    private fun expression() = openParen() || literal()

    private fun literal(): Boolean {
        val token = rd.consume { it is Token.Literal }

        if (token != null) {
            tokens.add(token)
            binaryOperator()
            closeParen()
            return binaryOperator() || closeParen() || true
        }

        return false
    }

    private fun binaryOperator(): Boolean {
        val token = rd.consume { it in validBinaryOperators }

        if (token != null) {
            tokens.add(token)
            return expression()
        }

        return false
    }

    private fun openParen(): Boolean {
        val token = rd.consume(L_PAREN)

        if (token != null) {
            parenIndent++

            return expression()
        }

        return false
    }

    private fun closeParen(): Boolean {
        val token = rd.consume(R_PAREN)

        if (token != null) {
            parenIndent--

            return binaryOperator() || parenIndent == 0
        }

        return false
    }
}