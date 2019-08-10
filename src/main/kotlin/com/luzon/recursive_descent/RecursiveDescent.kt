package com.luzon.recursive_descent

import com.luzon.exceptions.ExpectedTokenException
import com.luzon.exceptions.TokenRuleException
import com.luzon.lexer.Token
import com.luzon.lexer.Token.Keyword
import com.luzon.lexer.Token.Literal
import com.luzon.lexer.Token.Symbol
import com.luzon.lexer.Token.TokenEnum
import com.luzon.recursive_descent.ast.ASTNode

// Main entry point from the lz file
class RecursiveDescent(private val rd: TokenRDStream) {
    // TODO: List of ASTNodes -> Can have functions outside of classes like Kotlin?
    fun parse(): ASTNode? = classDefinition()

    // fun name(): Int { }
    private fun functionDefinition(): ASTNode? {
        fun parameterList(): List<ASTNode.FunctionParameter> {
            val params = mutableListOf<ASTNode.FunctionParameter>()

            do {
                val id = rd.accept(Literal.IDENTIFIER)

                if (id != null && rd.matchConsume(Symbol.TYPE)) {
                    val type = rd.accept(Literal.IDENTIFIER)

                    if (type != null)
                        params.add(ASTNode.FunctionParameter(id.data, type.data))
                }
            } while (rd.matchConsume(Symbol.COMMA))

            return params
        }

        if (rd.matchConsume(Keyword.FUN)) {
            val id = expect(Literal.IDENTIFIER)

            expect(Symbol.L_PAREN)

            val paramList = parameterList()

            expect(Symbol.R_PAREN)

            var type: Token? = null
            if (rd.matchConsume(Symbol.TYPE))
                type = expect(Literal.IDENTIFIER)

            val block = functionBlock()

            if (block != null)
                return ASTNode.FunctionDefinition(id.data, paramList, type?.data, block)
        }
        return null
    }

    @Throws(ExpectedTokenException::class)
    private fun expect(vararg tokens: TokenEnum) = rd.accept(*tokens)
        ?: throw ExpectedTokenException(rd.consume(), *tokens)

    private fun returnStatement(): ASTNode? {
        return if (rd.matchConsume(Keyword.RETURN))
            ASTNode.Return(expression())
        else null
    }

    // class <name>(<params>): <type> { }
    private fun classDefinition(): ASTNode? {
        fun parameterList(): List<ASTNode.ConstructorVariableDeclaration> {
            val params = mutableListOf<ASTNode.ConstructorVariableDeclaration>()

            do {
                val valVar = rd.accept(Keyword.VAL, Keyword.VAR)

                if (valVar != null) {
                    val id = rd.accept(Literal.IDENTIFIER)

                    if (id != null && rd.matchConsume(Symbol.TYPE)) {
                        val type = rd.accept(Literal.IDENTIFIER)

                        if (type != null)
                            params.add(ASTNode.ConstructorVariableDeclaration(id.data, type.data, valVar.tokenEnum == Keyword.VAL))
                    }
                }
            } while (rd.matchConsume(Symbol.COMMA))

            return params
        }

        if (rd.matchConsume(Keyword.CLASS)) {
            val id = expect(Literal.IDENTIFIER)
            var constructor: ASTNode.Constructor? = null

            if (rd.matchConsume(Symbol.L_PAREN)) {
                val parameters = parameterList()

                expect(Symbol.R_PAREN)
                constructor = ASTNode.Constructor(parameters)
            }

            val block = classBlock()

            if (block != null)
                return ASTNode.Class(id.data, constructor, block)
        }

        return null
    }

    private fun functionBlock() = lineOrBlock(::functionStatement)
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

    private fun lineOrBlock(stmt: () -> ASTNode?): ASTNode.Block? {
        val block = generalBlock(stmt)
        return if (block != null) block
        else {
            val statement = stmt()
            if (statement != null) ASTNode.Block(listOf(statement))
            else null
        }
    }

    // statements accepted within classes
    // secondary constructor // TODO: Secondary Constructor
    private fun classStatement() = acceptAny(::statement)

    // statements accepted within functions
    // return, loops, if
    private fun functionStatement() = acceptAny(::variableAssign, ::forLoop, ::whileLoop, ::doWhileLoop, ::ifStatement, ::returnStatement, ::statement)

    // statements accepted anywhere
    // variables, function def, class def?
    private fun statement() = acceptAny(::variableDeclaration, ::functionDefinition, ::classDefinition, ::expression)

    @Throws(TokenRuleException::class)
    private fun acceptAny(vararg nodes: () -> ASTNode?): ASTNode? {
        nodes.forEach {
            val result = it()
            if (result != null)
                return result
        }

        throw TokenRuleException(nodes.joinToString {
            val split = it.toString().split(":")[0].split(".").last()
            split.substring(0 until split.length - 2)
        })
    }

    // if(expr) { } else if(expr) { } else { }
    private fun ifStatement(): ASTNode.IfStatement? {
        if (rd.matchConsume(Keyword.IF)) {
            expect(Symbol.L_PAREN)
            val expr = expression()
            if (expr != null) {
                expect(Symbol.R_PAREN)
                val block = functionBlock()

                if (block != null) {
                    if (rd.matchConsume(Keyword.ELSE)) {
                        val ifStatement = ifStatement()

                        if (ifStatement != null)
                            return ASTNode.IfStatement(expr, block, ASTNode.ElseStatements.ElseIfStatement(ifStatement))

                        val elseBlock = functionBlock()
                        if (elseBlock != null)
                            return ASTNode.IfStatement(expr, block, ASTNode.ElseStatements.ElseStatement(elseBlock))
                    }

                    return ASTNode.IfStatement(expr, block, null)
                }
            }
        }
        return null
    }

    // for(i in 0..5) { }
    private fun forLoop(): ASTNode? {
        if (rd.matchConsume(Keyword.FOR)) {
            expect(Symbol.L_PAREN)
            val id = expect(Literal.IDENTIFIER)

            expect(Keyword.IN)

            val start = expect(Literal.INT)
            expect(Symbol.RANGE)
            val end = expect(Literal.INT)

            expect(Symbol.R_PAREN)
            val block = functionBlock()

            if (block != null)
                return ASTNode.ForLoop(id.data, start.data.toInt(), end.data.toInt(), block)
        }
        return null
    }

    private fun expression() = PrecedenceClimbing(rd).parse()

    // while(expr) { }
    private fun whileLoop(): ASTNode? {
        if (rd.matchConsume(Keyword.WHILE)) {
            expect(Symbol.L_PAREN)
            val expr = expression()

            if (expr != null) {
                expect(Symbol.R_PAREN)
                val block = functionBlock()

                if (block != null)
                    return ASTNode.WhileLoop(false, expr, block)
            }
        }
        return null
    }

    // do { } while(expr)
    private fun doWhileLoop(): ASTNode? {
        if (rd.matchConsume(Keyword.DO)) {
            val block = functionBlock()

            if (block != null) {
                expect(Keyword.WHILE)
                expect(Symbol.L_PAREN)
                val expr = expression()

                if (expr != null) {
                    expect(Symbol.R_PAREN)
                    return ASTNode.WhileLoop(true, expr, block)
                }
            }
        }

        return null
    }

    // val i: Int = 0
    private fun variableDeclaration(): ASTNode? {
        val varVal = rd.accept(Keyword.VAR, Keyword.VAL)

        if (varVal != null) {
            val constant = varVal.tokenEnum == Keyword.VAL
            val id = expect(Literal.IDENTIFIER)
            var type: Token? = null

            if (rd.matchConsume(Symbol.TYPE))
                type = expect(Literal.IDENTIFIER)

            expect(Symbol.EQUAL)
            val expr = expression()

            if (expr != null)
                return ASTNode.VariableDeclaration(id.data, type?.data, expr, constant)
        }
        return null
    }

    // i = 5, i += 5 etc.
    private fun variableAssign(): ASTNode? {
        if (rd.lookaheadMatches(Symbol.EQUAL, Symbol.MULTIPLY_ASSIGN, Symbol.DIVIDE_ASSIGN, Symbol.MOD_ASSIGN, Symbol.PLUS_ASSIGN, Symbol.SUBTRACT_ASSIGN)) {
            val id = expect(Literal.IDENTIFIER)

            val operator = rd.consume()?.tokenEnum as Symbol
            val expr = expression()

            if (expr != null) {
                if (operator == Symbol.EQUAL)
                    return ASTNode.VariableAssign(id.data, expr)
                return ASTNode.OperatorVariableAssign(id.data, expr, operator) // TODO: Check for specific operators still here
            }
        }
        return null
    }
}
