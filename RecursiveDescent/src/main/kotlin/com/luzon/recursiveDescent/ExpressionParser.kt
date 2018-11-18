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

    private fun Token.toExpression(): Expression? {
        return when (tokenEnum) {
            is Token.Literal -> {
                when (tokenEnum) {
                    Literal.INT -> IntLiteral.fromToken(this)
                    Literal.FLOAT -> FloatLiteral.fromToken(this)
                    Literal.DOUBLE -> DoubleLiteral.fromToken(this)
                    Literal.IDENTIFIER -> IdentifierLiteral.fromToken(this)
                    else -> null
                }
            }
            in binaryOperators -> {
                val op2 = stack.pop()
                val op1 = stack.pop()

                when (tokenEnum) { // TODO: Kotlin didn't like it the method referencing way, so this is the only way I can do it atm.
                    PLUS -> Expression.Binary.PlusExpr(op1, op2)
                    SUBTRACT -> Expression.Binary.SubExpr(op1, op2)
                    MULTIPLY -> Expression.Binary.MultExpr(op1, op2)
                    DIVIDE -> Expression.Binary.DivExpr(op1, op2)
                    else -> null
                }

//                expr!!(op1, op2)
            }
            else -> null
        }
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