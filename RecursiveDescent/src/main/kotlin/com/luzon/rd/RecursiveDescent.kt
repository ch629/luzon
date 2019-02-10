package com.luzon.rd

import com.luzon.lexer.Token
import com.luzon.lexer.Token.*
import com.luzon.rd.ast.ASTNode

// Main entry point from the lz file
class RecursiveDescent(val rd: TokenRDStream) {
    // TODO: List of ASTNodes -> Can have functions outside of classes like Kotlin?
    fun parse(): ASTNode? = classDefinition()

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

                    val block = functionBlock()

                    if (block != null)
                        return ASTNode.FunctionDefinition(id.data, paramList, type?.data, block)
                }
            }
        }
        return null
    }

    // class <name>(<params>): <type> { }
    private fun classDefinition(): ASTNode? {
        fun parameterList(): List<ASTNode.ConstructorVariableDeclaration> {
            val params = mutableListOf<ASTNode.ConstructorVariableDeclaration>()

            do {
                val valVar = rd.accept(Keyword.VAL, Keyword.VAR)

                if (valVar != null) {
                    val id = rd.accept(Literal.IDENTIFIER)

                    if (id != null) {
                        if (rd.matchConsume(Symbol.TYPE)) {
                            val type = rd.accept(Literal.IDENTIFIER)

                            if (type != null)
                                params.add(ASTNode.ConstructorVariableDeclaration(id.data, type.data, valVar.tokenEnum == Keyword.VAL))
                        }
                    }
                }
            } while (rd.matchConsume(Symbol.COMMA))

            return params
        }

        if (rd.matchConsume(Keyword.CLASS)) {
            val id = rd.accept(Literal.IDENTIFIER)
            var constructor: ASTNode.Constructor? = null

            if (id != null) {
                if (rd.matchConsume(Symbol.L_PAREN)) {
                    val parameters = parameterList()

                    if (rd.matchConsume(Symbol.R_PAREN))
                        constructor = ASTNode.Constructor(parameters)
                }

                val block = classBlock()

                if (block != null)
                    return ASTNode.Class(id.data, constructor, block)
            }
        }

        return null
    }

    private fun functionBlock() = generalBlock(::functionStatement)
    private fun classBlock() = generalBlock(::classStatement)
    private fun block() = generalBlock(::statement)

    private fun generalBlock(stmt: () -> ASTNode?): ASTNode.Block? {
        if (rd.matchConsume(Symbol.L_BRACE)) {
            val lines = mutableListOf<ASTNode>()

            while (!rd.matchConsume(Symbol.R_BRACE)) {
                val statement = stmt() ?: return null

                lines.add(statement)
            }

            return ASTNode.Block(lines)
        }
        return null
    }

    // statements accepted within classes
    // secondary constructor // TODO: Secondary Constructor
    private fun classStatement() = acceptAny(::statement)

    // statements accepted within functions
    // return, loops, if // TODO: Return statement
    private fun functionStatement() = acceptAny(::statement, ::variableAssign, ::forLoop, ::whileLoop, ::doWhileLoop, ::ifStatement)

    // statements accepted anywhere
    // variables, function def, class def?
    private fun statement() = acceptAny(::variableDeclaration, ::functionDefinition, ::classDefinition, ::expression)

    private fun acceptAny(vararg nodes: () -> ASTNode?): ASTNode? {
        nodes.forEach {
            val result = it()
            if (result != null)
                return result
        }
        return null
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

            if (expr != null && rd.matchConsume(Symbol.R_PAREN)) {
                val block = block()

                if (block != null)
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

    // i = 5, i += 5 etc.
    private fun variableAssign(): ASTNode? {
        if (rd.lookaheadMatches(Symbol.EQUAL, Symbol.MULTIPLY_ASSIGN, Symbol.DIVIDE_ASSIGN, Symbol.MOD_ASSIGN, Symbol.PLUS_ASSIGN, Symbol.SUBTRACT_ASSIGN)) {
            val id = rd.accept(Literal.IDENTIFIER)

            if (id != null) {
                val operator = rd.consume()?.tokenEnum as Symbol
                val expr = expression()

                if (expr != null) {
                    if (operator == Symbol.EQUAL)
                        return ASTNode.VariableAssign(id.data, expr)
                    return ASTNode.OperatorVariableAssign(id.data, expr, operator)
                }
            }
        }
        return null
    }
}