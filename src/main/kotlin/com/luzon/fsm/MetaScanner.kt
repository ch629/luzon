package com.luzon.fsm

import com.luzon.utils.Predicate
import com.luzon.utils.equalPredicate
import com.luzon.utils.errorWithException
import com.luzon.utils.orPredicate
import mu.NamedKLogging

abstract class MetaScanner<Alphabet : Any, Output>(text: List<Alphabet>, endValue: Alphabet) : Scanner<Alphabet>(text, endValue) {
    private val root = State<Alphabet, Output>()
    protected var endState = root
    protected var metaScope = root
    private var orScope = root
    private var scopeChange = false
    private var orState = State<Alphabet, Output>()
    private val orEndState = State<Alphabet, Output>()
    private var afterOr = false
    private var afterMeta = false

    protected abstract val orPredicate: Predicate<Alphabet>
    protected abstract val kleeneStarPredicate: Predicate<Alphabet>
    protected abstract val kleenePlusPredicate: Predicate<Alphabet>
    protected abstract val optionalPredicate: Predicate<Alphabet>
    protected abstract val startGroupPredicate: Predicate<Alphabet>
    protected abstract val endGroupPredicate: Predicate<Alphabet>
    protected abstract val escapePredicate: Predicate<Alphabet>
    private var isMetaPredicate: Predicate<Alphabet>? = null

    companion object : NamedKLogging("MetaScanner-Logger")

    abstract fun createScanner(text: List<Alphabet>): MetaScanner<Alphabet, Output>
    protected open fun customCharacters(char: Alphabet): StatePair<Alphabet, Output>? = null
    protected open fun unescapedCharacters(char: Alphabet): Predicate<Alphabet>? = null
    protected open fun escapedCharacters(char: Alphabet): Predicate<Alphabet>? = null

    fun toFSM(): State<Alphabet, Output> {
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

        endState.forceAccept = true

        return root
    }

    private fun char(escape: Boolean = false): StatePair<Alphabet, Output> {
        val charEnd = State<Alphabet, Output>(forceAccept = true)
        val char = advance()
        var isRange = true

        val unescapedCharacter = unescapedCharacters(char)
        val escapedCharacter = escapedCharacters(char)

        val predicate: Predicate<Alphabet> =
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

    private fun isMeta(char: Alphabet): Boolean {
        if (isMetaPredicate == null)
            isMetaPredicate =
                    orPredicate(orPredicate, kleeneStarPredicate,
                            kleenePlusPredicate, optionalPredicate,
                            startGroupPredicate, endGroupPredicate)

        return isMetaPredicate!!(char)
    }

    protected data class StatePair<Alphabet : Any, Output>(val start: State<Alphabet, Output>,
                                                           val end: State<Alphabet, Output>)

    protected infix fun State<Alphabet, Output>.to(other: State<Alphabet, Output>) = StatePair(this, other)

    private fun metaCharacter(): StatePair<Alphabet, Output> {
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
    private fun group(): StatePair<Alphabet, Output> {
        val scanner = createScanner(advanceUntil(endGroupPredicate))
        val states = scanner.toFSM()
        endState.addEpsilonTransition(states)
        metaScope = states

        return states to scanner.endState
    }

    //A|B|C
    private fun or(): StatePair<Alphabet, Output> {
        val extraState = State<Alphabet, Output>()
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
    private fun kleeneStar(): StatePair<Alphabet, Output> {
        val newEndState = State<Alphabet, Output>(forceAccept = true)

        endState.addEpsilonTransition(metaScope)
        metaScope.addEpsilonTransition(newEndState)

        return metaScope to newEndState
    }

    //A+
    private fun kleenePlus(): StatePair<Alphabet, Output> {
        val newEndState = State<Alphabet, Output>(forceAccept = true)

        endState.addEpsilonTransition(metaScope)
        endState.addEpsilonTransition(newEndState)

        return metaScope to newEndState
    }

    //A?
    private fun optional(): StatePair<Alphabet, Output> {
        val newEndState = State<Alphabet, Output>(forceAccept = true)

        metaScope.addEpsilonTransition(newEndState)
        endState.addEpsilonTransition(newEndState)

        return metaScope to newEndState
    }

    private fun advanceUntil(predicate: Predicate<Alphabet>): List<Alphabet> {
        val characters = mutableListOf<Alphabet>()

        while (true) {
            val currentChar = advance()

            if (predicate(currentChar)) break

            if (isAtEnd())
                logger.errorWithException("advancedUntil hit the end before passing the predicate.")

            characters.add(currentChar)
        }

        return characters
    }

    private fun hasOr() = !orState.isLeaf()
}