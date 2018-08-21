package com.luzon.fsm

import com.luzon.lexer.Scanner
import com.luzon.utils.errorWithException
import com.luzon.utils.or
import com.luzon.utils.predicate
import com.luzon.utils.range
import mu.NamedKLogging

class RegexScanner<T>(regex: String) : Scanner(regex) {
    private val root = State<T>()
    private var endState = root
    private var metaScope = root
    private var orScope = root
    private var scopeChange = false
    private var orState = State<T>()
    private val orEndState = State<T>()
    private var afterOr = false
    private var afterMeta = false

    companion object : NamedKLogging("Regex-Logger") {
        private const val META_CHARACTERS = "*+?|"
        private const val BRACKET_CHARACTERS = "()[]"
        private val numericalPredicate = '0' range '9'
        private val alphaNumericPredicate = numericalPredicate or ('A' range 'Z') or ('a' range 'z')
        private val anyCharacterPredicate: (Char) -> Boolean = { it != '\n' } //TODO: Could make a new predicate for this, but this just simplifies problems with newlines
        val unescapedCharacters = hashMapOf(
                '.' to anyCharacterPredicate
        )

        val escapedCharacters = hashMapOf(
                'd' to numericalPredicate,
                'w' to alphaNumericPredicate
        )
    }

    fun toFSM(): State<T> {
        while (!isAtEnd()) {
            var escape = false
            var char = peek()

            if (scopeChange) {
                metaScope = endState
                scopeChange = false
            }

            if (char == '\\') {
                escape = true
                advance()
                char = peek()
            }

            val startEnd = if (!escape) {
                when (char) {
                    '[' -> orBlock()
                    '(' -> parenthesis()
                    in META_CHARACTERS -> metaCharacter()
                    else -> char()
                }
            } else char(escape)

            endState = startEnd.second
            endState.removeAccept()

            if (afterOr) {
                endState = startEnd.first
                orScope = endState
                afterOr = false
            }
        }

        if (hasOr()) {
            endState.addEpsilonTransition(orEndState)
            endState = orEndState
        }

        endState.forceAccept = true

        return root
    }

    private fun char(escape: Boolean = false): Pair<State<T>, State<T>> {
        val charEnd = State<T>(forceAccept = true)
        val char = advance()
        var isRange = true

        val predicate =
                if (!escape && char in unescapedCharacters.keys) unescapedCharacters[char]!!
                else if (escape && char in escapedCharacters.keys) escapedCharacters[char]!!
                else {
                    isRange = false

                    if (escape && char !in unescapedCharacters.keys && char !in META_CHARACTERS && char !in BRACKET_CHARACTERS)
                        logger.warn("There is no escaped meaning to the character '$char'.")

                    char.predicate()
                }

        endState.addTransition(predicate, charEnd)

        if (afterMeta || isRange) {
            afterMeta = false
            metaScope = endState
        }

        return endState to charEnd
    }

    private fun orBlock(): Pair<State<T>, State<T>> {
//        afterMeta = true
        val end = State<T>(forceAccept = true)
        var transitionPredicate: (Char) -> Boolean = { false }

        advance() //Consume '['

        do {
            var char = advance()
            var escape = false
            if (char == '\\') {
                escape = true
                char = advance()
            }

            transitionPredicate = if (!escape && peek() == '-') { //Is Range
                advance() //Consume '-'
                transitionPredicate or (char range advance())
            } else {
                if (escape || char !in unescapedCharacters) transitionPredicate or char.predicate()
                else transitionPredicate or unescapedCharacters[char]!!
            }

        } while (peek() != ']' && !isAtEnd())

        advance() //Consume ']'

        endState.addTransition(transitionPredicate, end)
        metaScope = endState
        return endState to end
    }

    private fun parenthesis(): Pair<State<T>, State<T>> {
        advance() //Consume '('
        val scanner = RegexScanner<T>(advanceUntil(')'))
        val states = scanner.toFSM()
        endState.addEpsilonTransition(states)
        metaScope = states

        return states to scanner.endState
    }

    private fun metaCharacter(): Pair<State<T>, State<T>> {
        afterMeta = true
        val char = advance()

        return when (char) {
            '|' -> or()
            '*' -> asterisk()
            '+' -> plus()
            '?' -> question()
            else -> logger.errorWithException("metaCharacter was called on an invalid character '$char'")
        }
    }

    private fun or(): Pair<State<T>, State<T>> {
        val extraState = State<T>()
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

    private fun asterisk(): Pair<State<T>, State<T>> {
        val newEndState = State<T>(forceAccept = true)

        endState.addEpsilonTransition(metaScope)
        metaScope.addEpsilonTransition(newEndState)

        return metaScope to newEndState
    }

    private fun plus(): Pair<State<T>, State<T>> {
        val newEndState = State<T>(forceAccept = true)

        endState.addEpsilonTransition(metaScope)
        endState.addEpsilonTransition(newEndState)

        return metaScope to newEndState
    }

    private fun question(): Pair<State<T>, State<T>> {
        val newEndState = State<T>(forceAccept = true)

        metaScope.addEpsilonTransition(newEndState)
        endState.addEpsilonTransition(newEndState)

        return metaScope to newEndState
    }

    private fun advanceUntil(char: Char) = advanceUntil(char) { it == char }

    private fun advanceUntil(c: Char, pred: (Char) -> Boolean): String {
        val sb = StringBuilder()

        while (true) {
            val char = advance()
            val predResult = pred(char)

            if (predResult) break
            if (isAtEnd()) logger.errorWithException("advancedUntil hit the end before passing the predicate $c.")

            sb.append(char)
        }

        return sb.toString()
    }

    private fun hasOr() = !orState.isLeaf()
}
