package com.luzon.fsm.scanner

import com.luzon.fsm.OutputState
import com.luzon.utils.Predicate
import com.luzon.utils.equalPredicate
import com.luzon.utils.errorWithException
import com.luzon.utils.orPredicate
import mu.NamedKLogging

abstract class MetaScanner<A : Any, O>(text: List<A>, endValue: A) : Scanner<A>(text, endValue) {
    private val root = OutputState<A, O>()
    protected var endState = root
    protected var metaScope = root
    private var orScope = root
    private var scopeChange = false
    private var orState = OutputState<A, O>()
    private val orEndState = OutputState<A, O>()
    private var afterOr = false
    private var afterMeta = false

    protected abstract val orPredicate: Predicate<A>
    protected abstract val kleeneStarPredicate: Predicate<A>
    protected abstract val kleenePlusPredicate: Predicate<A>
    protected abstract val optionalPredicate: Predicate<A>
    protected abstract val startGroupPredicate: Predicate<A>
    protected abstract val endGroupPredicate: Predicate<A>
    protected abstract val escapePredicate: Predicate<A>
    private var isMetaPredicate: Predicate<A>? = null

    companion object : NamedKLogging("MetaScanner-Logger")

    abstract fun createScanner(text: List<A>): MetaScanner<A, O>
    protected open fun customCharacters(char: A): StatePair<A, O>? = null
    protected open fun unescapedCharacters(char: A): Predicate<A>? = null
    protected open fun escapedCharacters(char: A): Predicate<A>? = null

    fun toFSM(): OutputState<A, O> {
        while (isNotAtEnd()) {
            var escape = false
            var char = peek()

            if (scopeChange) {
                metaScope = endState
                scopeChange = false
            }

            if (escapePredicate(char)) {
                escape = true
                advance()
                char = peek()
            }

            val startEnd = if (!escape) {
                val customCharacters = customCharacters(char)
                when {
                    customCharacters != null -> customCharacters
                    isMeta(char) -> metaCharacter()
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

        endState.accepting = true

        return root
    }

    private fun char(escape: Boolean = false): StatePair<A, O> {
        val charEnd = OutputState<A, O>(accepting = true)
        val char = advance()
        var isRange = true

        val unescapedCharacter = unescapedCharacters(char)
        val escapedCharacter = escapedCharacters(char)

        val predicate: Predicate<A> =
                if (!escape && unescapedCharacter != null) unescapedCharacter
                else if (escape && escapedCharacter != null) escapedCharacter
                else {
                    isRange = false
                    char.equalPredicate()
                }

        endState.addTransition(predicate, charEnd)

        if (afterMeta || isRange) {
            afterMeta = false
            metaScope = endState
        }

        return endState to charEnd
    }

    private fun isMeta(char: A): Boolean {
        if (isMetaPredicate == null)
            isMetaPredicate =
                    orPredicate(orPredicate, kleeneStarPredicate,
                            kleenePlusPredicate, optionalPredicate,
                            startGroupPredicate, endGroupPredicate)

        return isMetaPredicate!!(char)
    }

    protected data class StatePair<A : Any, O>(val start: OutputState<A, O>, val end: OutputState<A, O>)

    protected infix fun OutputState<A, O>.to(other: OutputState<A, O>) = StatePair(this, other)

    private fun metaCharacter(): StatePair<A, O> {
        afterMeta = true
        val char = advance()

        return when {
            orPredicate(char) -> or()
            kleeneStarPredicate(char) -> kleeneStar()
            kleenePlusPredicate(char) -> kleenePlus()
            optionalPredicate(char) -> optional()
            startGroupPredicate(char) -> group()
            else -> RegexScanner.logger.errorWithException("metaCharacter was called on an invalid character '$char'")
        }
    }

    //(ABC)
    private fun group(): StatePair<A, O> {
        val scanner = createScanner(advanceUntil(endGroupPredicate))
        val states = scanner.toFSM()
        endState.addEpsilonTransition(states)
        metaScope = states

        return states to scanner.endState
    }

    //A|B|C
    private fun or(): StatePair<A, O> {
        val extraState = OutputState<A, O>()
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
    private fun kleeneStar(): StatePair<A, O> {
        val newEndState = OutputState<A, O>(accepting = true)

        endState.addEpsilonTransition(metaScope)
        metaScope.addEpsilonTransition(newEndState)

        return metaScope to newEndState
    }

    //A+
    private fun kleenePlus(): StatePair<A, O> {
        val newEndState = OutputState<A, O>(accepting = true)

        endState.addEpsilonTransition(metaScope)
        endState.addEpsilonTransition(newEndState)

        return metaScope to newEndState
    }

    //A?
    private fun optional(): StatePair<A, O> {
        val newEndState = OutputState<A, O>(accepting = true)

        metaScope.addEpsilonTransition(newEndState)
        endState.addEpsilonTransition(newEndState)

        return metaScope to newEndState
    }

    private fun advanceUntil(predicate: Predicate<A>): List<A> {
        val characters = mutableListOf<A>()

        while (true) {
            val currentChar = advance()

            if (predicate(currentChar)) break

            if (isAtEnd())
                logger.errorWithException("advancedUntil hit the end before passing the predicate.")

            characters.add(currentChar)
        }

        return characters
    }

    private fun hasOr() = !orState.leaf
}