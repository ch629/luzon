package com.luzon.recursive_descent.expression

import com.luzon.lexer.Token
import com.luzon.recursive_descent.PrecedenceClimbing
import com.luzon.recursive_descent.RecursiveParser
import com.luzon.recursive_descent.TokenRDStream
import com.luzon.recursive_descent.ast.ASTNode.Expression

internal class FunctionCallParser(private var name: String, rd: TokenRDStream) : RecursiveParser<Expression.LiteralExpr.FunctionCall>(rd) {
    private val params = mutableListOf<Expression>()

    override fun parse() = if (openParen()) Expression.LiteralExpr.FunctionCall(name, params.toList()) else null

    private fun openParen() = rd.matchConsume(Token.Symbol.L_PAREN) && (endParen() || parameter())
    private fun endParen() = rd.matchConsume(Token.Symbol.R_PAREN)

    private fun parameter(): Boolean {
        val expr = PrecedenceClimbing(rd).parse()

        if (expr != null) {
            params.add(expr)

            return endParen() || comma()
        }

        return false
    }

    private fun comma() = rd.matchConsume(Token.Symbol.COMMA) && parameter()
}