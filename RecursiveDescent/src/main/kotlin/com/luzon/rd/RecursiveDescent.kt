package com.luzon.rd

import com.luzon.lexer.Token
import com.luzon.lexer.Token.*
import com.luzon.rd.ast.ASTNode

// Main entry point from the lz file

class RecursiveDescent(val rd: TokenRDStream) {
    // TODO: List of ASTNodes -> Can have multiple functions and classes defined in the same file?
    fun parse(): ASTNode? = functionDefinition() ?: classDefinition()

    // TODO: Use either for error logging?
    // fun name(): Int { }
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

            if (id != null && rd.matchConsume(Symbol.L_PAREN)) {
                val paramList = parameterList()

                if (rd.matchConsume(Symbol.R_PAREN)) {
                    var type: Token? = null
                    if (rd.matchConsume(Symbol.TYPE))
                        type = rd.accept(Literal.IDENTIFIER) // TODO: Could maybe make accept throw an Exception for the error? -> Expected IDENTIFIER found ...

                    val block = block()

                    if (block != null)
                        return ASTNode.FunctionDefinition(id.data, paramList, type?.data, block)
                }
            }
        }
        return null
    }

    private fun classDefinition(): ASTNode? {
        TODO()
    }

    private fun block(): ASTNode.Block? {
        if (rd.matchConsume(Symbol.L_BRACE)) {
            // Find statements

            if (rd.matchConsume(Symbol.R_BRACE)) {
                // Return block
            }
        }
        TODO()
    }

    // TODO: Separate function statements and class statements?
    private fun statement(): ASTNode? {
        TODO()
    }

    // if(expr) { } else if(expr) { } else { }
    private fun ifStatement(): ASTNode.IfStatement? {
        if (rd.matchConsume(Keyword.IF) && rd.matchConsume(Symbol.L_PAREN)) {
            val expr = expression()
            if (expr != null && rd.matchConsume(Symbol.R_PAREN)) {
                val block = block()

                if (block != null) {
                    if (rd.matchConsume(Keyword.ELSE)) {
                        val ifStatement = ifStatement()

                        if (ifStatement != null)
                            return ASTNode.IfStatement(expr, block, ASTNode.ElseStatements.ElseIfStatement(ifStatement))

                        val elseBlock = block()
                        if (elseBlock != null)
                            return ASTNode.IfStatement(expr, block, ASTNode.ElseStatements.ElseStatement(elseBlock))
                    }

                    return ASTNode.IfStatement(expr, block, null)
                }
            }
        }
        return null
    }

    // TODO: This is a really basic for loop implementation, the real one would probably have to be more complex
    // for(i in 0..5) { }
    private fun forLoop(): ASTNode? {
        if (rd.matchConsume(Keyword.FOR) && rd.matchConsume(Symbol.L_PAREN)) {
            val id = rd.accept(Literal.IDENTIFIER)

            if (id != null && rd.matchConsume(Keyword.IN)) {
                val start = rd.accept(Literal.INT)
                if (start != null && rd.matchConsume(Symbol.RANGE)) {
                    val end = rd.accept(Literal.INT)

                    if (end != null) {
                        val block = block()

                        if (block != null)
                            return ASTNode.ForLoop(id.data, start.data.toInt(), end.data.toInt(), block)
                    }
                }
            }
        }
        return null
    }

    private fun expression() = PrecedenceClimbing(rd).parse()

    // while(expr) { }
    private fun whileLoop(): ASTNode? {
        if (rd.matchConsume(Keyword.WHILE) && rd.matchConsume(Symbol.L_PAREN)) {
            val expr = expression()

            if (expr != null && rd.matchConsume(Symbol.R_PAREN) && rd.matchConsume(Symbol.L_BRACE)) {
                val block = block()

                if (block != null && rd.matchConsume(Symbol.R_BRACE))
                    return ASTNode.WhileLoop(false, expr, block)
            }
        }
        return null
    }

    // do { } while(expr)
    private fun doWhileLoop(): ASTNode? {
        if (rd.matchConsume(Keyword.DO)) {
            val block = block()

            if (block != null && rd.matchConsume(Keyword.WHILE) && rd.matchConsume(Symbol.L_PAREN)) {
                val expr = expression()

                if (expr != null && rd.matchConsume(Symbol.R_PAREN))
                    return ASTNode.WhileLoop(true, expr, block)
            }
        }

        return null
    }

    // val i: Int = 0
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
                    val expr = expression()

                    if (expr != null)
                        return ASTNode.VariableDeclaration(id.data, type?.data, expr, constant)
                }
            }
        }
        return null
    }

    private fun variableAssign(): ASTNode? {
        if (rd.lookaheadMatches(Symbol.EQUAL)) {
            val id = rd.accept(Literal.IDENTIFIER)

            if (id != null) {
                rd.consume() // Consume Symbol.EQUAL
                val expr = expression()

                if (expr != null)
                    return ASTNode.VariableAssign(id.data, expr)
            }
        }
        return null
    }
}