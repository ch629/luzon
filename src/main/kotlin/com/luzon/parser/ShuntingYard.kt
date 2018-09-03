package com.luzon.parser

import com.luzon.lexer.Token
import com.luzon.lexer.Token.Literal
import com.luzon.lexer.Token.Symbol
import java.util.*

//From https://en.wikipedia.org/wiki/Shunting-yard_algorithm
//Orders expression in Reverse Polish Notation with precedence

fun main(args: Array<String>) {
    println(ShuntingYard.fromTokenSequence(sequenceOf(
            Literal.INT.toToken("5"),
            Symbol.PLUS.toToken(),
            Literal.INT.toToken("1"),
            Symbol.MULTIPLY.toToken(),
            Literal.INT.toToken("3")
    )))
}

class ShuntingYard {
    private var output = mutableListOf<Token>()
    private val operatorStack = Stack<Token>()

    companion object {
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
    }

    fun addOperator(oper: Token) {
        when (oper.tokenEnum) {
            Symbol.L_PAREN -> operatorStack.push(oper)
            Symbol.R_PAREN -> {
                while (operatorStack.peek().tokenEnum != Symbol.L_PAREN) //TODO: Check that a L_PAREN exists, otherwise error with invalid expression
                    output.add(operatorStack.pop())
                if (operatorStack.peek().tokenEnum == Symbol.L_PAREN)
                    operatorStack.pop() //Pop the L_PAREN
                else TODO("Error, unmatched parenthesis in expression")
            }
            else -> { //Operator
                var peek = if (operatorStack.isEmpty()) null else operatorStack.peek()
                while (peek != null && (peek.prec() < oper.prec() || (peek.prec() == oper.prec() && peek.leftAssociative())) && peek.tokenEnum != Symbol.L_PAREN) {
                    //Higher precedence in stack
                    output.add(operatorStack.pop())
                    peek = if (operatorStack.isEmpty()) null else operatorStack.peek()
                }
                operatorStack.push(oper)
            }
        }
    }

    fun addLiteral(token: Token) {
        output.add(token) //TODO: Function calls would be interpreted as IDENTIFIER L_PAREN R_PAREN? This might have to be ran after a single AST pass?
    }

    fun done(): ShuntingYard {
        while (operatorStack.isNotEmpty())
            output.add(operatorStack.pop())
        return this
    }

    override fun toString(): String { //TODO: Get Output (Token List)
        return output.joinToString(" ")
    }

    fun getOutput(): List<Token> = output.toList()

    fun toAST(): Expression {
        val stack = Stack<Expression>()

        output.forEach {
            //TODO: Replace this with a while stack is not empty
            if (it.tokenEnum is Literal)  //TODO: Or Identifier/Function Call
                stack.push(LiteralExpression(it))
            else { //TODO: stack.size >= 2
                val op1 = stack.pop()
                val op2 = stack.pop() //TODO: Depends on unary or binary operation

                if (it.tokenEnum is Symbol) stack.push(BinaryExpression(it.tokenEnum, op1, op2))
            }
        }

        return stack.pop()
    }

    private fun Token.prec() = when (tokenEnum) {
        Symbol.L_PAREN,
        Symbol.R_PAREN -> 0
        Symbol.DIVIDE,
        Symbol.MULTIPLY -> 1
        Symbol.PLUS,
        Symbol.SUBTRACT -> 2
        else -> 3
    }

    private fun Token.leftAssociative() = when (tokenEnum) {
        Symbol.DIVIDE,
        Symbol.SUBTRACT -> true
        else -> false
    }
}