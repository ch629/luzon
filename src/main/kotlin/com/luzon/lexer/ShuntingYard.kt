package com.luzon.lexer

import java.util.*

//From https://en.wikipedia.org/wiki/Shunting-yard_algorithm
//Orders expression in Reverse Polish Notation with precedence
class ShuntingYard {
    private var output = mutableListOf<Token>()
    private val operatorStack = Stack<Token>()

    companion object {
        fun fromString(str: String): ShuntingYard {
            val yard = ShuntingYard()
            str.split(" ").forEach { yard.add(it) }
            yard.done()
            return yard
        }

        fun fromTokenSequence(seq: Sequence<Token>): ShuntingYard {
            val yard = ShuntingYard()
            seq.forEach {
                when (it.tokenEnum) {
                    is Symbol -> yard.addOperator(it)
                    is Literal -> yard.addLiteral(it)
                }
            }

            yard.done()
            return yard
        }
    }

    fun add(inp: String) {
        val oper: Symbol? = when (inp) {
            "+" -> Symbol.PLUS
            "-" -> Symbol.SUBTRACT
            "*" -> Symbol.MULTIPLY
            "/" -> Symbol.DIVIDE
            "(" -> Symbol.L_PAREN
            ")" -> Symbol.R_PAREN
            else -> null
        }

        if (oper != null) addOperator(oper.toToken())
    }

    fun addOperator(oper: Token) {
        when (oper.tokenEnum) {
            Symbol.L_PAREN -> operatorStack.push(oper)
            Symbol.R_PAREN -> {
                while (operatorStack.peek().tokenEnum != Symbol.L_PAREN) //TODO: Check that a L_PAREN exists, otherwise error with invalid expression
                    output.add(operatorStack.pop())
                operatorStack.pop() //Pop the L_PAREN
            }
            else -> { //Operator
                var peek = if (operatorStack.isEmpty()) null else operatorStack.peek()
                while (peek != null && (prec(peek) < prec(oper) || (prec(peek) == prec(oper) && leftAssociative(peek))) && peek.tokenEnum != Symbol.L_PAREN) {
                    //Higher precedence in stack
                    output.add(operatorStack.pop())
                    peek = if (operatorStack.isEmpty()) null else operatorStack.peek()
                }
                operatorStack.push(oper)
            }
        }
    }

    fun addLiteral(token: Token) {
        output.add(token) //TODO: Correct value here dependant on literal
    }

    fun done() {
        while (operatorStack.isNotEmpty())
            output.add(operatorStack.pop())
    }

    override fun toString(): String {
        return output.joinToString(" ")
    }

    private fun prec(symbol: Token) = when (symbol.tokenEnum) {
        Symbol.L_PAREN,
        Symbol.R_PAREN -> 0
        Symbol.DIVIDE,
        Symbol.MULTIPLY -> 1
        Symbol.PLUS,
        Symbol.SUBTRACT -> 2
        else -> 3
    }

    private fun leftAssociative(symbol: Token) = when (symbol.tokenEnum) {
        Symbol.DIVIDE,
        Symbol.SUBTRACT -> true
        else -> false
    }
}