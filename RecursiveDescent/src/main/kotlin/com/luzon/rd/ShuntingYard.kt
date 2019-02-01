package com.luzon.rd

import com.luzon.lexer.Token
import com.luzon.lexer.Token.Literal
import com.luzon.lexer.Token.Symbol
import com.luzon.lexer.TokenStream
import com.luzon.utils.peekOrNull
import java.util.*

// From https://en.wikipedia.org/wiki/Shunting-yard_algorithm
// Orders expression in Reverse Polish Notation with precedence

class ShuntingYard {
    private var output = mutableListOf<Token>()
    private val operatorStack = Stack<Token>()

    companion object {
        fun fromTokenSequence(seq: TokenStream): ShuntingYard {
            val yard = ShuntingYard()
            seq.forEach {
                when (it.tokenEnum) {
                    is Symbol -> yard.addOperator(it)
                    is Literal, is Token.CustomEnum -> yard.addLiteral(it)
                }
            }

            return yard.done()
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
                else throw Exception("Expression has mismatching parentheses") //TODO: Use Either to log this error to the user?
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
        if (token.tokenEnum == Token.CustomEnum && token !is FunctionCallToken)
            return

        output.add(token)
    }

    fun done() = apply {
        while (operatorStack.isNotEmpty())
            output.add(operatorStack.pop())
    }

    override fun toString() = output.joinToString(" ")

    fun getOutput() = output.toList()

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