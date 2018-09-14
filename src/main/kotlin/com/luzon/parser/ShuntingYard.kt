package com.luzon.parser

import com.luzon.lexer.Token
import com.luzon.lexer.Token.Literal
import com.luzon.lexer.Token.Symbol
import com.luzon.utils.peekOrNull
import java.util.*

// From https://en.wikipedia.org/wiki/Shunting-yard_algorithm
// Orders expression in Reverse Polish Notation with precedence

// TODO: Convert this to work with an AST
// One solution would be to traverse the tree and recreate the token stream with some
// ASTNodes (Identifier, Function Call, etc) then put it through the Shunting Yard then back
// through the parser to create the AST
class ShuntingYard {
    private var output = mutableListOf<Token>()
    private val operatorStack = Stack<Token>()

    companion object {
        // Probably removed once this is converted to AST, as I need to work with function calls which would be read as
        // IDENTIFIER, L_PAREN, R_PAREN without any parameters, and with could include other INTs which would confuse
        // the ShuntingYard -> Potentially make an internal class to handle these appropriately.
        fun fromTokenSequence(seq: Sequence<Token>): ShuntingYard {
            val yard = ShuntingYard()
            seq.forEach {
                when (it.tokenEnum) {
                    is Symbol -> yard.addOperator(it)
                    is Literal -> yard.addLiteral(it)
                }
            }

            return yard.done()
        }

        // Convert into a Stream of singular values, operators, functions etc.
        // Put into the ShuntingYard
        fun fromAST(expr: Expression): ShuntingYard {
            TODO()
        }
    }

    fun addOperator(operator: Token) {
        when (operator.tokenEnum) {
            Symbol.L_PAREN -> operatorStack.push(operator)
            Symbol.R_PAREN -> {
                while (operatorStack.peek().tokenEnum != Symbol.L_PAREN)
                    output.add(operatorStack.pop())
                if (operatorStack.peek().tokenEnum == Symbol.L_PAREN)
                    operatorStack.pop() //Pop the L_PAREN
                else throw Exception("Expression didn't have matching parenthesis") //TODO: Use Either to log this error to the user?
            }
            else -> { // Operator
                var peek = operatorStack.peekOrNull()
                while (peek != null && shouldAddOutput(operator, peek)) { // Higher precedence in stack
                    output.add(operatorStack.pop())
                    peek = operatorStack.peekOrNull()
                }
                operatorStack.push(operator)
            }
        }
    }

    // Just to make it more legible
    private fun shouldAddOutput(operator: Token, peekToken: Token) =
            (peekToken.precedence < operator.precedence
                    || (peekToken.precedence == operator.precedence && peekToken.leftAssociative))
                    && peekToken.tokenEnum != Symbol.L_PAREN


    fun addLiteral(token: Token) {
        output.add(token)
    }

    fun done(): ShuntingYard = apply {
        while (operatorStack.isNotEmpty())
            output.add(operatorStack.pop())
    }

    override fun toString() = output.joinToString(" ")

    fun getOutput() = output.toList()

    fun toAST(): Expression {
        TODO("Run the output though the Parser")
    }

    private val Token.precedence
        get() = when (tokenEnum) {
            Symbol.L_PAREN,
            Symbol.R_PAREN -> 0
            Symbol.DIVIDE,
            Symbol.MULTIPLY -> 1
            Symbol.PLUS,
            Symbol.SUBTRACT -> 2
            else -> 3
        }

    private val Token.leftAssociative
        get() = when (tokenEnum) {
            Symbol.DIVIDE,
            Symbol.SUBTRACT -> true
            else -> false
        }
}