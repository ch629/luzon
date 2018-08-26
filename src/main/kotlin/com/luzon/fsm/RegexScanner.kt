package com.luzon.fsm

import com.luzon.utils.or
import com.luzon.utils.predicate
import com.luzon.utils.range
import mu.NamedKLogging

class RegexScanner<Output>(regex: String) : MetaCharScanner<Output>(regex) { //TODO: Might need to modify this to work with AST creation too
    companion object : NamedKLogging("Regex-Logger") {
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

    override fun createScanner(text: String) = RegexScanner<Output>(text)

    override fun toFSM(): State<Char, Output> {
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
                    in META_CHARACTERS -> metaCharacter()
                    else -> char()
                }
            } else char(escape)

            endState = startEnd.end
            endState.removeAccept()

            if (afterOr) {
                endState = startEnd.start
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

    private fun char(escape: Boolean = false): StatePair<Output> {
        val charEnd = State<Char, Output>(forceAccept = true)
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

    //[ABC]
    private fun orBlock(): StatePair<Output> {
        val end = State<Char, Output>(forceAccept = true)
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
}
