package com.luzon.fsm

import com.luzon.utils.errorWithException
import com.luzon.utils.or
import mu.NamedKLogging

abstract class MetaScanner<Alphabet, Output>(text: List<Alphabet>, endValue: Alphabet) : Scanner<Alphabet>(text, endValue) {
    private val root = State<Alphabet, Output>()
    protected var endState = root
    protected var metaScope = root
    private var orScope = root
    private var scopeChange = false
    private var orState = State<Alphabet, Output>()
    private val orEndState = State<Alphabet, Output>()
    private var afterOr = false
    private var afterMeta = false

    protected abstract val orPredicate: (Alphabet) -> Boolean
    protected abstract val kleeneStarPredicate: (Alphabet) -> Boolean
    protected abstract val kleenePlusPredicate: (Alphabet) -> Boolean
    protected abstract val optionalPredicate: (Alphabet) -> Boolean
    protected abstract val startGroupPredicate: (Alphabet) -> Boolean
    protected abstract val endGroupPredicate: (Alphabet) -> Boolean
    protected abstract val escapePredicate: (Alphabet) -> Boolean
    private var isMetaPredicate: ((Alphabet) -> Boolean)? = null

    companion object : NamedKLogging("MetaScanner-Logger")

    abstract fun createScanner(text: List<Alphabet>): MetaScanner<Alphabet, Output>
    protected open fun customCharacters(char: Alphabet): StatePair<Alphabet, Output>? = null
    protected open fun unescapedCharacters(char: Alphabet): ((Alphabet) -> Boolean)? = null
    protected open fun escapedCharacters(char: Alphabet): ((Alphabet) -> Boolean)? = null

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

        val predicate: (Alphabet) -> Boolean =
                if (!escape && unescapedCharacter != null) unescapedCharacter
                else if (escape && escapedCharacter != null) escapedCharacter
                else {
                    isRange = false
                    val pred: (Alphabet) -> Boolean = { it == char }
                    pred
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
            isMetaPredicate = orPredicate or kleeneStarPredicate or kleenePlusPredicate or optionalPredicate or startGroupPredicate or endGroupPredicate

        return isMetaPredicate!!(char)
    }

    protected data class StatePair<Alphabet, Output>(val start: State<Alphabet, Output>,
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


    protected fun advanceUntil(predicate: (Alphabet) -> Boolean): List<Alphabet> {
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