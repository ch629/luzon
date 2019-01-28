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
    val and = AND.toToken()
    val or = OR.toToken()
    val greater = GREATER.toToken()
    val lesser = LESS.toToken()
    val TRUE = Literal.BOOLEAN.toToken("true")
    val FALSE = Literal.BOOLEAN.toToken("false")

    // Numerical
    val c = parseExpression(sequenceOf(id("apple"), plus, int(2)))
    val d = parseExpression(sequenceOf(id("func"), lParen, int(2), plus, int(3), rParen, plus, int(5)))

    // Boolean
    val e = BooleanExpressionRecognizer.recognizeExpression(sequenceOf(lParen, FALSE, or, TRUE, rParen))
    val f = BooleanExpressionRecognizer.recognizeExpression(sequenceOf(int(2), greater, int(5)))

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

    fun getExpressionTokens() = if (expression()) tokens.asSequence() else null // TODO: parenIndent == 0??

    private fun expression() = openParen() || literal()

    private fun literal(): Boolean {
        val token = rd.consume { it is Literal && it != Literal.BOOLEAN }

        if (token != null) {
            tokens.add(token)
            // TODO: Maybe exclude identifier from this, and use a separate rule; like I use in the boolean version
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
        if (rd.consume(L_PAREN) != null) {
            parenIndent++

            return expression()
        }

        return false
    }

    private fun closeParen(): Boolean {
        if (parenIndent <= 0) return false // TODO: This avoids the need for the buffer, but isn't ideal.

        if (rd.consume(R_PAREN) != null) {
            parenIndent--
            // TODO: The problem is that the R_PAREN is consumed, so the function can't be created.
            //  -> I need to maybe match, then consume it, if the next part passes somehow
            //  -> Or put it in some sort of buffer which the recursiveDescent checks first
            // This would some sort of revert command to put back into the buffer
            // Or I just need more lookahead to check before consuming

            return binaryOperator() || closeParen() || parenIndent == 0
        }

        return false
    }

    private fun functionCall(id: String) = functionCallGeneral(rd, tokens, id, { binaryOperator() || closeParen() || true })
}

private fun functionCallGeneral(rd: RecursiveDescent, tokens: MutableList<Token>, id: String, callback: () -> Boolean): Boolean {
    if (rd.consume(L_PAREN) != null) {
        val params = mutableListOf<TokenStream>()
        val firstParam = ExpressionRecognizer.recognizeExpression(rd)

        if (firstParam != null) {
            firstParam.toList().forEach { println(it) }
            params.add(firstParam)

            while (rd.consume(COMMA) != null) {
                val param = ExpressionRecognizer.recognizeExpression(rd)
                if (param != null) params.add(param)
                // else error -> Unexpected comma
            }
        }

        if (rd.consume(R_PAREN) != null) {
            tokens.add(FunctionCallToken(id, params))

            return callback()
        } // else error -> Mismatched parentheses
    }

    return false
}

internal class BooleanExpressionRecognizer(private val rd: RecursiveDescent) { // TODO: expression >= other expression?
    private val tokens = mutableListOf<Token>()
    private var parenIndent = 0
    private var not = false

    companion object {
        private val validBinaryOperators = listOf(AND, OR, GREATER, LESS, GREATER_EQUAL, LESS_EQUAL, EQUAL_EQUAL, NOT_EQUAL)

        fun recognizeExpression(rd: RecursiveDescent) = BooleanExpressionRecognizer(rd).getExpressionTokens()
        fun recognizeExpression(tokens: TokenStream) = recognizeExpression(RecursiveDescent(tokens))
    }

    fun getExpressionTokens() = if (booleanExpression()) tokens.asSequence() else null // TODO: parenIndent == 0??

    private fun booleanExpression() = not() || literal() || openParen() || numericalExpression() || identifier()

    private fun numericalExpression(): Boolean {
        val exprTokens = ExpressionRecognizer.recognizeExpression(rd)

        if (exprTokens != null) {
            tokens.addAll(exprTokens)
            val lastToken = tokens.last()

            return binaryOperator() || closeParen() || lastToken is FunctionCallToken || lastToken.tokenEnum == Literal.IDENTIFIER
        }

        return false
    }

    private fun literal(): Boolean {
        val token = rd.consume(Literal.BOOLEAN)

        if (token != null) {
            tokens.add(token)
            return closeParen() || binaryOperator() || true
        }

        return false
    }

    private fun identifier(): Boolean {
        val token = rd.consume(Literal.IDENTIFIER)

        if (token != null) {
            if (!functionCall(token.data)) tokens.add(token)

            return closeParen() || binaryOperator() || true
        }

        return false
    }

    private fun not(): Boolean {
        if (rd.consume(NOT) != null) {
            not = true // TODO: This probably wont work correctly
            return literal() || identifier() || openParen()
        }

        return false
    }

    private fun binaryOperator(): Boolean {
        val token = rd.consume { it in validBinaryOperators }

        if (token != null) {
            tokens.add(token)

            return booleanExpression()
        }

        return false
    }

    private fun openParen(): Boolean {
        if (rd.consume(L_PAREN) != null) {
            tokens.add(L_PAREN.toToken())
            parenIndent++

            if (not) {
                val expr = recognizeExpression(rd)
                not = false

                return expr != null && tokens.add(NotExpressionToken(expr))
            }

            return booleanExpression()
        }

        return false
    }

    private fun closeParen(): Boolean {
        if (parenIndent <= 0) return false // TODO: This avoids the need for the buffer, but isn't ideal.

        if (rd.consume(R_PAREN) != null) {
            tokens.add(R_PAREN.toToken())
            parenIndent--
            return binaryOperator() || closeParen() || parenIndent == 0
        }

        return false
    }

    private fun functionCall(id: String) = functionCallGeneral(rd, tokens, id, { binaryOperator() || closeParen() || true })
}

internal data class FunctionCallToken(val id: String, val params: List<TokenStream>) : Token.Custom()
internal data class NotExpressionToken(val value: TokenStream) : Token.Custom()