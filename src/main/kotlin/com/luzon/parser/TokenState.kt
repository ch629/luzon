package com.luzon.parser

import com.luzon.lexer.Token
import com.luzon.lexer.Token.Literal.*
import com.luzon.utils.Predicate
import com.luzon.utils.getConstructorParameters
import com.luzon.utils.tryConstructorArgs
import java.util.*

//TODO: Create a DSL for generating the parser ASTNode classes.
//TODO: Create FSM & States

data class Transition<T>(val predicate: Predicate<T>, val stateTo: TokenState)
typealias TerminalTransition = Transition<Token>
typealias NonTerminalTransition = Transition<ASTNode>

class TokenState(private val node: ASTNode, private val first: Boolean = false) {
    private val terminalTransitions = mutableListOf<TerminalTransition>()
    private val nonTerminalTransitions = mutableListOf<NonTerminalTransition>()

    fun acceptTerminals(token: Token) =
            terminalTransitions.asSequence().filter { it.predicate(token) }.map { it.stateTo }.distinct().toList()

    fun acceptNonTerminals(node: ASTNode) =
            nonTerminalTransitions.asSequence().filter { it.predicate(node) }.map { it.stateTo }.distinct().toList()

    // We should only need the waiting transitions if it's this is the first state of the FSM to avoid recursively
    // pushing this non-terminal onto the FSM stack
    fun getWaitingTransitions() =
            if (first) nonTerminalTransitions.asSequence().filter { it.predicate(node) }.toList() else emptyList()
}

class TokenFSM {
    val states = mutableListOf<TokenState>()
    val waiting = mutableListOf<NonTerminalTransition>()
}

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