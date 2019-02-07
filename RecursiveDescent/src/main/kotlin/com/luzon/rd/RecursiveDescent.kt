package com.luzon.rd

import com.luzon.lexer.Token
import com.luzon.lexer.Token.*
import com.luzon.rd.ast.ASTNode

// Main entry point from the lz file

class RecursiveDescent(val rd: TokenRDStream) {
    // TODO: List of ASTNodes -> Can have multiple functions and classes defined in the same file?
    fun parse(): ASTNode? = functionDefinition() ?: classDefinition()

    private fun functionDefinition(): ASTNode? {
        fun parameterList(): List<ASTNode.FunctionParameter> {
            val params = mutableListOf<ASTNode.FunctionParameter>()

            do {
                val id = rd.accept(Literal.IDENTIFIER)

                if (id != null) {
                    if (rd.matchConsume(Symbol.TYPE)) {
                        val type = rd.accept(Literal.IDENTIFIER)

                        if (type != null)
                            params.add(ASTNode.FunctionParameter(id.data, type.data))
                    }
                }
            } while (rd.matchConsume(Symbol.COMMA))

            return params
        }

        if (rd.matchConsume(Keyword.FUN)) { // TODO: Maybe add an expect, so I don't need as many levels of if statements
            val id = rd.accept(Literal.IDENTIFIER)

            if (rd.matchConsume(Symbol.L_PAREN)) {
                val paramList = parameterList()

                if (rd.matchConsume(Symbol.R_PAREN)) {
                    var type: Token? = null
                    if (rd.matchConsume(Symbol.TYPE))
                        type = rd.accept(Literal.IDENTIFIER) // TODO: Could maybe make accept throw an Exception for the error? -> Expected IDENTIFIER found ...

                    if (rd.matchConsume(Symbol.L_BRACE)) {
                        // Block
                        if (rd.matchConsume(Symbol.R_BRACE)) {
                            // Return Function
                        }
                    }
                }
            }
        }
        TODO()
    }

    private fun classDefinition(): ASTNode? {
        TODO()
    }

    private fun statement(): ASTNode? {
        TODO()
    }

    private fun variableDeclaration(): ASTNode? {
        val varVal = rd.accept(Keyword.VAR, Keyword.VAL)

        if (varVal != null) {
            val constant = varVal.tokenEnum == Keyword.VAL
            val id = rd.accept(Literal.IDENTIFIER)
            if (id != null) {
                var type: Token? = null

                if (rd.matchConsume(Symbol.TYPE))
                    type = rd.accept(Literal.IDENTIFIER)

                if (rd.matchConsume(Symbol.EQUAL)) {
                    val expr = PrecedenceClimbing(rd).parse()

                    if (expr != null)
                        return ASTNode.VariableDeclaration(id.data, type?.data, expr, constant)
                }
            }
        }

        return null
    }
}