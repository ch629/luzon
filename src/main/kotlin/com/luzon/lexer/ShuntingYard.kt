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
            str.split(" ").forEach { yard.add(it) } //TODO: Better way of adding each token (So spaces aren't needed between every one)
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
            else -> { //Operator
                var peek = if (operatorStack.isEmpty()) null else operatorStack.peek()
                while (peek != null && (peek.prec < oper.prec || (peek.prec == oper.prec && peek.leftAssociative)) && peek != LPAREN) {
                    //Higher precedence in stack
                    output.add(operatorStack.pop())
                    peek = if (operatorStack.isEmpty()) null else operatorStack.peek()
                }
                operatorStack.push(oper)
            }
        }
    }

    fun done() {
        while (operatorStack.isNotEmpty())
            output.add(operatorStack.pop())
    }

    override fun toString(): String {
        return output.joinToString(separator = " ")
    }

    enum class ExprOperators(val prec: Int, val leftAssociative: Boolean = false) {
        LPAREN(0),
        RPAREN(0),
        DIVIDE(1, true),
        MULTIPLY(1),
        PLUS(2),
        MINUS(2, true)
    }
}