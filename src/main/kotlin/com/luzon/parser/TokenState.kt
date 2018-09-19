package com.luzon.parser

import com.luzon.lexer.Token
import com.luzon.lexer.Token.Literal.*
import com.luzon.utils.Predicate
import com.luzon.utils.merge
import com.luzon.utils.popOrNull
import com.luzon.utils.replaceWith
import java.util.*
import kotlin.reflect.KFunction

// TODO: Create a DSL for generating the parser ASTNode classes.
// TODO: Create FSM & States (Either from the DSL or from an ASTNode class)

data class Transition<T, O>(val predicate: Predicate<T>, val stateTo: TokenState<O>)
typealias TerminalTransition<O> = Transition<Token, O>
typealias NonTerminalTransition<O> = Transition<ASTNode, O>

class TokenState<O>(private val node: ASTNode, private val first: Boolean = false, private val output: KFunction<O>? = null) {
    private val terminalTransitions = mutableListOf<TerminalTransition<O>>()
    private val nonTerminalTransitions = mutableListOf<NonTerminalTransition<O>>()

    fun acceptTerminals(token: Token) =
            terminalTransitions.asSequence()
                    .filter { it.predicate(token) }
                    .map { it.stateTo }
                    .distinct().toList()

    fun acceptNonTerminals(node: ASTNode) =
            nonTerminalTransitions.asSequence()
                    .filter { it.predicate(node) }
                    .map { it.stateTo }
                    .distinct().toList()

    // We should only need the waiting transitions if it's this is the first state of the FSM to avoid recursively
    // pushing this non-terminal onto the FSM stack
    fun getWaitingTransitions() =
            if (first) nonTerminalTransitions.asSequence()
                    .filter { it.predicate(node) }.toList()
            else emptyList()

    fun call(vararg params: Any) = output?.call(*params)
    fun isAccepting() = output != null
}

class TokenFSM<O>(private val states: MutableList<TokenState<O>>) {
    private val originalStates = states.toList()
    val waiting = mutableListOf<NonTerminalTransition<O>>()

    fun accept(token: Token): Boolean {
        val newStates = states.map { it.acceptTerminals(token) }.merge().distinct()

        states.replaceWith(newStates)

        return newStates.isNotEmpty()
    }

    fun reset() {
        states.replaceWith(originalStates)
    }
}

private fun <T> Stack<T>.reduce(amount: Int, filter: Predicate<T> = { true }) = reduceStack(this, amount, filter)
private fun <T> reduceStack(dataStack: Stack<T>, amount: Int, filter: Predicate<T> = { true }): Stack<T> {
    val newStack = Stack<T>()
    (0 until amount).forEach { _ ->
        val data = dataStack.pop()
        if (filter(data))
            newStack.push(data)
    }

    newStack.reverse()
    return newStack
}

private fun <T> reduceFilterSkip(dataStack: Stack<T>, amount: Int, skip: Predicate<T> = { false }): Stack<T> {
    val newStack = Stack<T>()
    var i = 0

    while (i < amount) {
        val data = dataStack.popOrNull() ?: break
        if (skip.invoke(data)) continue
        newStack.push(data)
        i++
    }

    newStack.reverse()
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

// TODO: Generate constructor FSM with extra non-terminals not in the constructor parameters
// PlusExpr = <Expr> PLUS <Expr> -> Where PLUS is not stored within the constructor
// This might mean that I need to generate the main FSM from the DSL, then filter the data out and
// run tryConstructorArgs to instantiate the class.
// Most of the time, terminal tokens are not used within the constructor
// Unless or'd -> then we generally want to turn that into a non-terminal within itself. (VAR | VAL)
// I might not want to use the tryConstructorArgs though, as I could make an FSM from the DSL which outputs
// a KFunction creating the class.
