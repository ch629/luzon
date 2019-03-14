package com.luzon.rd.expression

import com.luzon.lexer.Token
import com.luzon.rd.ast.ASTNode.Expression

typealias ExpressionStream = Sequence<ExpressionToken>

class ExpressionStreamList {
    private val expression = mutableListOf<ExpressionToken>()

    fun toStream(): ExpressionStream = expression.asSequence()

    fun add(functionCall: Expression.LiteralExpr.FunctionCall) {
        expression.add(ExpressionToken.FunctionCall(functionCall))
    }

    fun add(token: Token, unary: Boolean = false) {
        var exprToken = ExpressionToken.fromToken(token)

        if (exprToken != null) {
            if (unary && exprToken is ExpressionToken.BinaryOperator) exprToken = exprToken.unary()
            expression.add(exprToken)
        }
    }

    fun add(symbol: Token.Symbol, unary: Boolean = false) {
        if (symbol == Token.Symbol.L_PAREN || symbol == Token.Symbol.R_PAREN) {
            expression.add(ExpressionToken.SymbolToken(symbol))
        } else expression.add(if (unary) ExpressionToken.UnaryOperator(symbol) else ExpressionToken.BinaryOperator(symbol))
    }

    operator fun plusAssign(symbol: Token.Symbol) = add(symbol)
    operator fun plusAssign(token: Token) = add(token)

    operator fun plusAssign(exprToken: ExpressionToken) {
        expression.add(exprToken)
    }
}

sealed class ExpressionToken {
    class ExpressionLiteral(val token: Token) : ExpressionToken()

    data class DotChain(val token: ExpressionToken, val next: DotChain? = null) : ExpressionToken()

    data class BinaryOperator(val symbol: Token.Symbol) : ExpressionToken() {
        fun unary() = UnaryOperator(symbol)
    }

    data class UnaryOperator(val symbol: Token.Symbol) : ExpressionToken()

    data class FunctionCall(val function: Expression.LiteralExpr.FunctionCall) : ExpressionToken()

    data class SymbolToken(val symbol: Token.Symbol) : ExpressionToken()

    companion object {
        fun fromToken(token: Token) = when (token.tokenEnum) {
            is Token.Literal -> ExpressionLiteral(token)
            is Token.Symbol -> when (token.tokenEnum) {
                in ExpressionRecognizer.binaryOperators -> BinaryOperator(token.tokenEnum)
                in ExpressionRecognizer.unaryOperators -> UnaryOperator(token.tokenEnum)
                else -> SymbolToken(token.tokenEnum)
            }
            else -> null
        }
    }

    val tokenType
        get() = when (this) {
            is ExpressionLiteral -> token.tokenEnum
            is BinaryOperator -> symbol
            is UnaryOperator -> symbol
            is SymbolToken -> symbol
            else -> null
        }
}