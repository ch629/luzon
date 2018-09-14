package com.luzon.parser

import com.luzon.lexer.Token
import com.luzon.lexer.Token.Literal.*
import com.luzon.utils.Predicate
import com.luzon.utils.getConstructorParameters
import com.luzon.utils.tryConstructorArgs
import java.util.*

//TODO: Create a DSL for generating the parser ASTNode classes.
//TODO: Create FSM & States

private fun <T> Stack<T>.reduce(amount: Int, vararg filters: Predicate<T>) = reduceStack(this, amount, *filters)
private fun <T> reduceStack(dataStack: Stack<T>, amount: Int, vararg filters: Predicate<T>): Stack<T> {
    val newStack = Stack<T>()
    (0 until amount).forEach { _ ->
        val data = dataStack.pop()
        if (filters.all { it(data) })
            newStack.push(data)
    }
    return newStack
}

private fun Token.toNode(): ASTNode? {
    return if (tokenEnum is Token.Literal) {
        TokenNode(when (tokenEnum) {
            DOUBLE -> data.toDouble() to DOUBLE
            FLOAT -> data.toFloat() to FLOAT
            INT -> data.toInt() to INT
            STRING -> makeString(data) to STRING
            CHAR -> makeChar(data) to CHAR
            BOOLEAN -> data.toBoolean() to BOOLEAN
            IDENTIFIER -> data to IDENTIFIER
        })
    } else null
}

private fun makeChar(string: String) = makeString(string)[0]
private fun makeString(string: String) = string.substring(1, string.length - 1)

fun main(args: Array<String>) {
    //i: Int
    val decl = tryConstructorArgs<VariableDeclaration>("i", "Int")!!
    println("VariableDeclaration: ${decl.name}: ${decl.type!!}")

    //b = 5
    val decl2 = tryConstructorArgs<VariableDeclaration>("b", LiteralExpression(Token.Literal.INT.toToken("5")))!!
    println("VariableDeclaration: ${decl2.name}")

    VariableDeclaration::class.getConstructorParameters().forEach { conParams ->
        println(conParams.joinToString { it.simpleName!! })
    }

    getConstructorParameters<VariableDeclaration>().forEach { conParams ->
        println(conParams.joinToString { it.simpleName!! })
    }
}