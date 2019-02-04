package com.luzon.rd.expression

import com.luzon.lexer.Token
import com.luzon.rd.PrecedenceClimbing
import com.luzon.rd.RecursiveParser
import com.luzon.rd.TokenRDStream
import com.luzon.rd.ast.Expression

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