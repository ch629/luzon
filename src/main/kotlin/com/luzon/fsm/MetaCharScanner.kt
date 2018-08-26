package com.luzon.fsm

import com.luzon.lexer.Scanner
import com.luzon.utils.errorWithException
import mu.NamedKLogging

abstract class MetaCharScanner<Output>(text: String) : Scanner(text) {
    protected val root = State<Char, Output>()
    protected var endState = root
    protected var metaScope = root
    protected var orScope = root
    protected var scopeChange = false
    private var orState = State<Char, Output>()
    protected val orEndState = State<Char, Output>()
    protected var afterOr = false
    protected var afterMeta = false

    companion object : NamedKLogging("MetaCharScanner-Logger") {
        const val META_CHARACTERS = "*+?|()"
        const val BRACKET_CHARACTERS = "()[]"
    }

    abstract fun toFSM(): State<Char, Output>
    abstract fun createScanner(text: String): MetaCharScanner<Output>

    protected data class StatePair<Output>(val start: State<Char, Output>,
                                           val end: State<Char, Output>)

    protected infix fun State<Char, Output>.to(other: State<Char, Output>) = StatePair(this, other)

    protected fun metaCharacter(): StatePair<Output> {
        afterMeta = true
        val char = advance()

        return when (char) {
            '|' -> or()
            '*' -> asterisk()
            '+' -> plus()
            '?' -> question()
            '(' -> parenthesis()
            else -> RegexScanner.logger.errorWithException("metaCharacter was called on an invalid character '$char'")
        }
    }

    //(ABC)
    private fun parenthesis(): StatePair<Output> {
        val scanner = createScanner(advanceUntil(')'))
        val states = scanner.toFSM()
        endState.addEpsilonTransition(states)
        metaScope = states

        return states to scanner.endState
    }

    //A|B|C
    private fun or(): StatePair<Output> {
        val extraState = State<Char, Output>()
        scopeChange = true
        afterOr = true

        if (!hasOr()) { //First or in regex
            val newState = orScope.transferToNext()
            orScope.replaceWith(orState)
            orState = orScope
            orScope.addEpsilonTransition(newState)
        } else orState.addEpsilonTransition(orScope)

        endState.addEpsilonTransition(orEndState)
        orState.addEpsilonTransition(extraState)

        return extraState to orEndState
    }

    //A*
    private fun asterisk(): StatePair<Output> {
        val newEndState = State<Char, Output>(forceAccept = true)

        endState.addEpsilonTransition(metaScope)
        metaScope.addEpsilonTransition(newEndState)

        return metaScope to newEndState
    }

    //A+
    private fun plus(): StatePair<Output> {
        val newEndState = State<Char, Output>(forceAccept = true)

        endState.addEpsilonTransition(metaScope)
        endState.addEpsilonTransition(newEndState)

        return metaScope to newEndState
    }

    //A?
    private fun question(): StatePair<Output> {
        val newEndState = State<Char, Output>(forceAccept = true)

        metaScope.addEpsilonTransition(newEndState)
        endState.addEpsilonTransition(newEndState)

        return metaScope to newEndState
    }


    private fun advanceUntil(foundChar: Char): String {
        val sb = StringBuilder()

        while (true) {
            val currentChat = advance()

            if (currentChat == foundChar) break
            if (isAtEnd())
                logger.errorWithException("advancedUntil hit the end before passing the predicate $foundChar.")

            sb.append(currentChat)
        }

        return sb.toString()
    }

    protected fun hasOr() = !orState.isLeaf()
}