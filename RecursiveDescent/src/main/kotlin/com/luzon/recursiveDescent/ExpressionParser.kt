package com.luzon.recursiveDescent

import com.luzon.lexer.Token
import com.luzon.lexer.Token.Literal
import com.luzon.lexer.Token.Symbol.*
import com.luzon.lexer.TokenStream
import com.luzon.recursiveDescent.ast.Expression
import com.luzon.recursiveDescent.ast.Expression.LiteralExpr.*
import com.luzon.utils.popOrNull
import java.util.*

fun main(args: Array<String>) {
    fun int(value: Int) = Literal.INT.toToken(value.toString())
    fun id(name: String) = Literal.IDENTIFIER.toToken(name)
    val plus = PLUS.toToken()
    val lParen = L_PAREN.toToken()
    val rParen = R_PAREN.toToken()

    val c = parseExpression(sequenceOf(id("apple"), plus, int(2)))

    val d = parseExpression(sequenceOf(id("func"), lParen, int(2), rParen, plus, int(5)))

    val i = 5
}

fun parseExpression(tokens: TokenStream) = ExpressionParser.fromUnsortedTokens(tokens).parse()

internal class ExpressionParser(private val tokens: TokenStream) {
    private val stack = Stack<Expression>()

    companion object {
        private val binaryOperators = listOf(PLUS, SUBTRACT, MULTIPLY, DIVIDE)

        fun fromUnsortedTokens(tokens: TokenStream) =
                ExpressionParser(ShuntingYard.fromTokenSequence(
                        ExpressionRecognizer.recognizeExpression(tokens) ?: emptySequence())
                        .getOutput()
                        .asSequence()
                )
    }

    private fun Token.toExpression() = when (tokenEnum) {
        is Token.Literal -> when (tokenEnum) {
            Literal.INT -> IntLiteral.Companion::fromToken
            Literal.FLOAT -> FloatLiteral.Companion::fromToken
            Literal.DOUBLE -> DoubleLiteral.Companion::fromToken
            Literal.IDENTIFIER -> IdentifierLiteral.Companion::fromToken
            else -> null
        }?.invoke(this)

        is Token.CustomEnum -> {
            if (this is FunctionCallToken) FunctionCall.fromToken(this)
            else null
        }

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

        fun recognizeExpression(rd: RecursiveDescent) = ExpressionRecognizer(rd).getExpressionTokens()
        fun recognizeExpression(tokens: TokenStream) = recognizeExpression(RecursiveDescent(tokens))
    }

    fun getExpressionTokens() = if (expression()) tokens.asSequence() else null

    private fun expression() = openParen() || literal()

    private fun literal(): Boolean {
        val token = rd.consume { it is Token.Literal }

        if (token != null) {
            tokens.add(token)

            return (token.tokenEnum == Literal.IDENTIFIER && functionCall(token.data)) || binaryOperator() || closeParen() || true
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
        if (parenIndent <= 0) return false // TODO: This avoids the need for the buffer, but isn't ideal.
        val token = rd.consume(R_PAREN)

        if (token != null) {
            parenIndent--
            // TODO: The problem is that the R_PAREN is consumed, so the function can't be created.
            //  -> I need to maybe match, then consume it, if the next part passes somehow
            //  -> Or put it in some sort of buffer which the recursiveDescent checks first
            // This would some sort of revert command to put back into the buffer
            // Or I just need more lookahead to check before consuming

            return binaryOperator() || parenIndent == 0
        }

        return false
    }

    private fun functionCall(id: String): Boolean {
        if (rd.consume(L_PAREN) != null) {
            val params = mutableListOf<TokenStream>()

            val firstParam = recognizeExpression(rd)
            if (firstParam != null) {
                firstParam.toList().forEach { println(it) }
                params.add(firstParam)

                while (rd.consume(COMMA) != null) {
                    val param = recognizeExpression(rd)
                    if (param != null) params.add(param)
                    // else error -> Unexpected comma
                }
            }

            if (rd.consume(R_PAREN) != null) {
                tokens.add(FunctionCallToken(id, params))

                return binaryOperator() || true
            } // else error -> Mismatched parentheses
        }

        return false
    }
}

internal data class FunctionCallToken(val id: String, val params: List<TokenStream>) : Token.Custom()