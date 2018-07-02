package com.luzon.lexer

import com.luzon.lexer.ShuntingYard.ExprOperators.*
import java.util.*

//From https://en.wikipedia.org/wiki/Shunting-yard_algorithm
//Orders expression in Reverse Polish Notation with precedence
class ShuntingYard {
    private var output = mutableListOf<Any>()
    private val operatorStack = Stack<ExprOperators>()

    companion object {
        fun fromString(str: String): ShuntingYard {
            val yard = ShuntingYard()
            //TODO: Split into each token and call add
            yard.done()
            return yard
        }
    }

    fun add(inp: String) {
        val oper: ExprOperators? = when (inp) {
            "+" -> PLUS
            "-" -> MINUS
            "*" -> MULTIPLY
            "/" -> DIVIDE
            "(" -> LPAREN
            ")" -> RPAREN
            else -> null
        }

        when (oper) {
            LPAREN -> operatorStack.push(LPAREN)
            RPAREN -> {
                while (operatorStack.peek() != LPAREN) //TODO: Check that a LPAREN exists, otherwise error with invalid expression
                    output.add(operatorStack.pop())
                operatorStack.pop() //Pop the LPAREN
            }
            null -> output.add(inp.toInt()) //TODO: Check Int or function call or identifier etc
            else -> {
                var peek = operatorStack.peek()
                while (peek.ordinal < oper.ordinal && peek != LPAREN) { //TODO: Left associative stuff (Only matters for MOD etc.)
                    //Higher precedence in stack
                    output.add(operatorStack.pop())
                    peek = operatorStack.peek()
                }
                operatorStack.push(oper)
            }
        }
    }

    fun done() {
        while (operatorStack.isNotEmpty())
            output.add(operatorStack.pop())
    }

    enum class ExprOperators {
        LPAREN,
        RPAREN,
        DIVIDE,
        MULTIPLY,
        PLUS,
        MINUS
    }
}