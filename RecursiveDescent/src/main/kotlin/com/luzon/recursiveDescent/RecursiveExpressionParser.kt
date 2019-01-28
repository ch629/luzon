package com.luzon.recursiveDescent

import com.luzon.lexer.Token
import com.luzon.lexer.Token.Literal
import com.luzon.lexer.Token.Symbol
import com.luzon.recursiveDescent.ast.Expression

// New Solution

/*
When current token is an operator and higher precedence than the root node's operator, place new operator on rhs of root
if the rhs of root is not null, place the value into the left of the new operator then place the operator on the rhs of root
if lower, place the root as the lhs of the new node.

If the token is a literal, and the root is null, set root to the literal
if the root is not null
    if the root's left value is null, set to the literal
    else set right to the literal
 */
// TODO: Left associativity

// TODO: Inner -> Inside of parentheses
internal class RecursiveExpressionParser(rd: RecursiveDescent, private val inner: Boolean = false) : RecursiveParser<Expression>(rd) {
    private var root: Expression? = null

    override fun parse() = if (parentheses() || unaryOperator() || literal()) root else null

    private fun setExpression(expr: Expression): Boolean { // TODO: May need to traverse the tree to find an empty node to insert into.
        when (expr) {
            is Expression.LiteralExpr -> {
                if (root == null) root = expr
                else {
                    when (root) {
                        is Expression.Binary -> {
                            if ((root as Expression.Binary).left == null) (root as Expression.Binary).left = expr
                            else (root as Expression.Binary).right = expr // TODO: Check if right is null too?
                        }
                        is Expression.Unary -> {
                            (root as Expression.Unary).expr = expr
                        }
                    }
                }
            }
            is Expression.Binary -> {
                if (root == null) root = expr
                else {
                    when (root) {
                        is Expression.LiteralExpr -> {
                            expr.left = root
                            root = expr
                            return true
                        }
                    }
                }
                TODO("Precedence")
            }
        }

        return true
    }

    private fun literal(): Boolean {
        val token = rd.accept { it is Token.Literal }

        if (token != null) {
            var expr = Expression.LiteralExpr.fromToken(token)

            if (token.tokenEnum == Literal.IDENTIFIER) {
                val functionCall = RecursiveFunctionCallParser(token.data, rd).parse()

                if (functionCall != null)
                    expr = functionCall
            }


            return expr != null && setExpression(expr) && (binaryOperator() || true)
        }

        return false
    }

    private fun parentheses(): Boolean {
        if (rd.matchConsume(Symbol.L_PAREN)) {
            val expr = RecursiveExpressionParser(rd, true).parse()

            if (expr != null && setExpression(expr))
                return binaryOperator() || true
        } else if (inner && rd.matchConsume(Symbol.R_PAREN)) {
            return true
        } else if (rd.matches(Symbol.R_PAREN)) return true

        return false
    }

    private fun unaryOperator(): Boolean {
        val op = rd.accept { it == Symbol.SUBTRACT || it == Symbol.NOT }

        if (op != null) {
            var expr: Expression? = null
            when (op.tokenEnum) {
                Symbol.SUBTRACT -> {
                    expr = Expression.Unary.SubExpr()
                }
                Symbol.NOT -> {
                    expr = Expression.Unary.NotExpr()
                }
            }

            setExpression(expr!!)

            return true
        }

        return false
    }

    private val binaryOperators = listOf(
            Symbol.SUBTRACT, Symbol.PLUS, Symbol.DIVIDE, Symbol.MULTIPLY,
            Symbol.GREATER, Symbol.GREATER_EQUAL, Symbol.LESS, Symbol.LESS_EQUAL,
            Symbol.EQUAL_EQUAL, Symbol.NOT_EQUAL, Symbol.AND, Symbol.OR
    )

    private fun binaryOperator(): Boolean {
        val op = rd.accept { it in binaryOperators }

        if (op != null) {
            setExpression(Expression.Binary.PlusExpr())
            return literal() || parentheses()
//            TODO()
        }

        return false
    }
}

fun main() {
    fun id(name: String) = Literal.IDENTIFIER.toToken(name)
    val lParen = Symbol.L_PAREN.toToken()
    val rParen = Symbol.R_PAREN.toToken()
    val comma = Symbol.COMMA.toToken()
    val plus = Symbol.PLUS.toToken()
    fun int(value: Int) = Literal.INT.toToken(value.toString())

    val tokens = sequenceOf(
            int(20), plus, id("test"), lParen, int(5), comma, int(2), rParen
    )

//    val funCall = RecursiveFunctionCallParser("test", RecursiveDescent(tokens)).parse()
    val expr = RecursiveExpressionParser(RecursiveDescent(tokens)).parse()

    val i = 0
}

internal class RecursiveFunctionCallParser(private var name: String, rd: RecursiveDescent) : RecursiveParser<Expression.LiteralExpr.FunctionCall>(rd) {
    private val params = mutableListOf<Expression>()

    override fun parse() = if (openParen()) Expression.LiteralExpr.FunctionCall(name, params.toList()) else null

    private fun openParen() = rd.matchConsume(Symbol.L_PAREN) && (endParen() || parameter())
    private fun endParen() = rd.matchConsume(Symbol.R_PAREN)

    private fun parameter(): Boolean {
        val expr = RecursiveExpressionParser(rd).parse()

        if (expr != null) {
            params.add(expr)
            return endParen() || comma()
        }

        return false
    }

    private fun comma() = rd.matchConsume(Symbol.COMMA) && parameter()
}

internal abstract class RecursiveParser<T>(val rd: RecursiveDescent) {
    abstract fun parse(): T?
}