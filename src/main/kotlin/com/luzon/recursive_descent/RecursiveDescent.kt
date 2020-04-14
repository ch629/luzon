package com.luzon.recursive_descent

import com.luzon.exceptions.TokenRuleException
import com.luzon.exceptions.UnexpectedTokenException
import com.luzon.lexer.Token
import com.luzon.lexer.Token.Keyword
import com.luzon.lexer.Token.Literal
import com.luzon.lexer.Token.Symbol
import com.luzon.lexer.Token.TokenEnum
import com.luzon.recursive_descent.ast.SyntaxTreeNode

// Main entry point from the lz file
class RecursiveDescent(private val rd: TokenRecursiveDescentStream) {
    // TODO: List of SyntaxTreeNode -> Can have functions outside of classes like Kotlin?
    fun parse(): SyntaxTreeNode? = classDefinition()

    // kotlin:S125
    // fun name(): Int { }
    private fun functionDefinition(): SyntaxTreeNode? {
        fun parameterList(): List<SyntaxTreeNode.FunctionParameter> {
            val params = mutableListOf<SyntaxTreeNode.FunctionParameter>()

            do {
                val id = rd.accept(Literal.IDENTIFIER)

                if (id != null && rd.matchConsume(Symbol.TYPE)) {
                    val type = rd.accept(Literal.IDENTIFIER)

                    if (type != null)
                        params.add(SyntaxTreeNode.FunctionParameter(id.data, type.data))
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
                return SyntaxTreeNode.FunctionDefinition(id.data, paramList, type?.data, block)
        }
        return null
    }

    @Throws(UnexpectedTokenException::class)
    private fun expect(vararg tokens: TokenEnum) = rd.accept(*tokens)
        ?: throw UnexpectedTokenException(rd.consume(), *tokens)

    private fun returnStatement(): SyntaxTreeNode? {
        return if (rd.matchConsume(Keyword.RETURN))
            SyntaxTreeNode.Return(expression())
        else null
    }

    // kotlin:S125
    // class <name>(<params>): <type> { }
    private fun classDefinition(): SyntaxTreeNode? {
        fun parameterList(): List<SyntaxTreeNode.ConstructorVariableDeclaration> {
            val params = mutableListOf<SyntaxTreeNode.ConstructorVariableDeclaration>()

            do {
                val valVar = rd.accept(Keyword.VAL, Keyword.VAR)

                if (valVar != null) {
                    val id = rd.accept(Literal.IDENTIFIER)

                    if (id != null && rd.matchConsume(Symbol.TYPE)) {
                        val type = rd.accept(Literal.IDENTIFIER)

                        if (type != null)
                            params.add(SyntaxTreeNode.ConstructorVariableDeclaration(id.data, type.data, valVar.tokenEnum == Keyword.VAL))
                    }
                }
            } while (rd.matchConsume(Symbol.COMMA))

            return params
        }

        if (rd.matchConsume(Keyword.CLASS)) {
            val id = expect(Literal.IDENTIFIER)
            var constructor: SyntaxTreeNode.Constructor? = null

            if (rd.matchConsume(Symbol.L_PAREN)) {
                val parameters = parameterList()

                expect(Symbol.R_PAREN)
                constructor = SyntaxTreeNode.Constructor(parameters)
            }

            return SyntaxTreeNode.Class(id.data, constructor, classBlock() ?: SyntaxTreeNode.Block(listOf()))
        }

        return null
    }

    private fun functionBlock() = lineOrBlock(::functionStatement)
    private fun classBlock() = generalBlock(::classStatement)
    private fun block() = generalBlock(::statement)

    private fun generalBlock(stmt: () -> SyntaxTreeNode?): SyntaxTreeNode.Block? {
        if (rd.matchConsume(Symbol.L_BRACE)) {
            val lines = mutableListOf<SyntaxTreeNode>()

            while (!rd.matchConsume(Symbol.R_BRACE)) {
                val statement = stmt() ?: return null

                lines.add(statement)
            }

            return SyntaxTreeNode.Block(lines)
        }
        return null
    }

    private fun lineOrBlock(stmt: () -> SyntaxTreeNode?): SyntaxTreeNode.Block? {
        val block = generalBlock(stmt)
        return if (block != null) block
        else {
            val statement = stmt()
            if (statement != null) SyntaxTreeNode.Block(listOf(statement))
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
    private fun acceptAny(vararg nodes: () -> SyntaxTreeNode?): SyntaxTreeNode? {
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

    // kotlin:S125
    // if(expr) { } else if(expr) { } else { }
    private fun ifStatement(): SyntaxTreeNode.IfStatement? {
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
                            return SyntaxTreeNode.IfStatement(expr, block, SyntaxTreeNode.ElseStatements.ElseIfStatement(ifStatement))

                        val elseBlock = functionBlock()
                        if (elseBlock != null)
                            return SyntaxTreeNode.IfStatement(expr, block, SyntaxTreeNode.ElseStatements.ElseStatement(elseBlock))
                    }

                    return SyntaxTreeNode.IfStatement(expr, block, null)
                }

                throw TokenRuleException("if statement block")
            }
        }
        return null
    }

    // kotlin:S125
    // for(i in 0..5) { }
    private fun forLoop(): SyntaxTreeNode? {
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
                return SyntaxTreeNode.ForLoop(id.data, start.data.toInt(), end.data.toInt(), block)

            throw TokenRuleException("for loop block")
        }
        return null
    }

    private fun expression() = PrecedenceClimbing(rd).parse()

    // kotlin:S125
    // while(expr) { }
    private fun whileLoop(): SyntaxTreeNode? {
        if (rd.matchConsume(Keyword.WHILE)) {
            expect(Symbol.L_PAREN)
            val expr = expression()

            if (expr != null) {
                expect(Symbol.R_PAREN)
                val block = functionBlock()

                if (block != null)
                    return SyntaxTreeNode.WhileLoop(false, expr, block)

                throw TokenRuleException("while loop block")
            }
        }
        return null
    }

    // kotlin:S125
    // do { } while(expr)
    private fun doWhileLoop(): SyntaxTreeNode? {
        if (rd.matchConsume(Keyword.DO)) {
            val block = functionBlock()

            if (block != null) {
                expect(Keyword.WHILE)
                expect(Symbol.L_PAREN)
                val expr = expression()

                if (expr != null) {
                    expect(Symbol.R_PAREN)
                    return SyntaxTreeNode.WhileLoop(true, expr, block)
                }

                throw TokenRuleException("expression")
            }
        }

        return null
    }

    // kotlin:S125
    // val i: Int = 0
    private fun variableDeclaration(): SyntaxTreeNode? {
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
                return SyntaxTreeNode.VariableDeclaration(id.data, type?.data, expr, constant)

            throw TokenRuleException("expression")
        }
        return null
    }

    // kotlin:S125
    // i = 5, i += 5 etc.
    private fun variableAssign(): SyntaxTreeNode? {
        if (rd.lookaheadMatches(Symbol.EQUAL, Symbol.MULTIPLY_ASSIGN, Symbol.DIVIDE_ASSIGN, Symbol.MOD_ASSIGN, Symbol.PLUS_ASSIGN, Symbol.SUBTRACT_ASSIGN)) {
            val id = expect(Literal.IDENTIFIER)

            val operator = rd.consume()?.tokenEnum as Symbol
            val expr = expression()

            if (expr != null) {
                if (operator == Symbol.EQUAL)
                    return SyntaxTreeNode.VariableAssign(id.data, expr)
                return SyntaxTreeNode.OperatorVariableAssign(id.data, expr, operator) // TODO: Check for specific operators still here
            }

            throw TokenRuleException("expression")
        }
        return null
    }
}
