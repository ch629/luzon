package com.luzon.fsm.scanner

import com.luzon.fsm.FSM
import com.luzon.fsm.State
import com.luzon.utils.*
import mu.NamedKLogging

class RegexScanner<O>(regex: List<Char>) : MetaScanner<Char, O>(regex, '\n') {
    constructor(regex: String) : this(regex.toCharList())

    override val orPredicate: Predicate<Char>
        get() = '|'.equalPredicate()
    override val kleeneStarPredicate: Predicate<Char>
        get() = '*'.equalPredicate()
    override val kleenePlusPredicate: Predicate<Char>
        get() = '+'.equalPredicate()
    override val optionalPredicate: Predicate<Char>
        get() = '?'.equalPredicate()
    override val startGroupPredicate: Predicate<Char>
        get() = '('.equalPredicate()
    override val endGroupPredicate: Predicate<Char>
        get() = ')'.equalPredicate()
    override val escapePredicate: Predicate<Char>
        get() = '\\'.equalPredicate()

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
        private val anyCharacterPredicate: Predicate<Char> = { it != '\n' }
    }

    override fun createScanner(text: List<Char>) = RegexScanner<O>(text)

    override fun customCharacters(char: Char) = when (char) {
        '[' -> orBlock()
        else -> null
    }

    //[ABC]
    private fun orBlock(): StatePair<Char, O> {
        val end = State<Char, O>(forceAccept = true)
        var transitionPredicate: Predicate<Char> = { false }

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
                else if (unescapedCharacter == null) transitionPredicate or char.equalPredicate()
                else transitionPredicate or unescapedCharacter
            }

        } while (peek() != ']' && isNotAtEnd())

        advance() //Consume ']'

        endState.addTransition(transitionPredicate, end)
        metaScope = endState
        return endState to end
    }
}

private val regexCache = hashMapOf<String, FSM<Char, Unit>>()
// Just a regular RegEx parser
internal fun regex(regex: String): RegexMatcher {
    if (!regexCache.containsKey(regex)) regexCache[regex] = FSM.fromRegex(regex)
    return RegexMatcher(regexCache[regex]!!.copyOriginal())
}

class RegexMatcher(private val stateMachine: FSM<Char, Unit>) {
    fun matches(input: String): Boolean {
        input.forEach { stateMachine.accept(it) }
        return stateMachine.accepting
    }
}