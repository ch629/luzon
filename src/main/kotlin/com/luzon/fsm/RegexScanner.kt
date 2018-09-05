package com.luzon.fsm

import com.luzon.utils.or
import com.luzon.utils.predicate
import com.luzon.utils.range
import com.luzon.utils.toCharList
import mu.NamedKLogging

class RegexScanner<Output>(regex: List<Char>) : MetaScanner<Char, Output>(regex, '\n') {
    constructor(regex: String) : this(regex.toCharList())

    override val orPredicate: (Char) -> Boolean
        get() = { it == '|' }
    override val kleeneStarPredicate: (Char) -> Boolean
        get() = { it == '*' }
    override val kleenePlusPredicate: (Char) -> Boolean
        get() = { it == '+' }
    override val optionalPredicate: (Char) -> Boolean
        get() = { it == '?' }
    override val startGroupPredicate: (Char) -> Boolean
        get() = { it == '(' }
    override val endGroupPredicate: (Char) -> Boolean
        get() = { it == ')' }
    override val escapePredicate: (Char) -> Boolean
        get() = { it == '\\' }

    override fun escapedCharacters(char: Char) = when (char) {
        'd' -> numericalPredicate
        'w' -> alphaNumericPredicate
        else -> null
    }

    override fun unescapedCharacters(char: Char) = when (char) {
        '.' -> anyCharacterPredicate
        else -> null
    }

    companion object : NamedKLogging("Regex-Logger") {
        private val numericalPredicate = '0' range '9'
        private val alphaNumericPredicate = numericalPredicate or ('A' range 'Z') or ('a' range 'z')
        private val anyCharacterPredicate: (Char) -> Boolean = { it != '\n' }
    }

    override fun createScanner(text: List<Char>) = RegexScanner<Output>(text)

    override fun customCharacters(char: Char) = when (char) {
        '[' -> orBlock()
        else -> null
    }

    //[ABC]
    private fun orBlock(): StatePair<Char, Output> {
        val end = State<Char, Output>(forceAccept = true)
        var transitionPredicate: (Char) -> Boolean = { false }

        advance() //Consume '['

        do {
            var char = advance()
            var escape = false
            if (escapePredicate(char)) {
                escape = true
                char = advance()
            }

            transitionPredicate = if (!escape && peek() == '-') { //Is Range
                advance() //Consume '-'
                transitionPredicate or (char range advance())
            } else { //Normal Character
                val unescapedCharacter = unescapedCharacters(char)
                val escapedCharacter = escapedCharacters(char)

                if (escape && escapedCharacter != null) transitionPredicate or escapedCharacter
                else if (unescapedCharacter == null) transitionPredicate or char.predicate()
                else transitionPredicate or unescapedCharacter
            }

        } while (peek() != ']' && isNotAtEnd())

        advance() //Consume ']'

        endState.addTransition(transitionPredicate, end)
        metaScope = endState
        return endState to end
    }
}
