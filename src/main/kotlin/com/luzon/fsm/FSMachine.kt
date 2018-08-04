package com.luzon.fsm

//TODO: Helper class to deal with inputs and exiting with possible outputs at a specific location; to consume the correct input into a token.
class FSMachine<T>(statesList: List<State<T>>) {
    constructor(root: State<T>) : this(mutableListOf(root))

    private val states = statesList.toMutableList()

    init {
        updateEpsilons()
    }

    companion object {
        fun <T> fromRegex(str: String) = FSMachine(RegexScanner<T>(str).toFSM())
    }

    private fun updateEpsilons(): Boolean {
        val epsilons = states.map { it.acceptEpsilons() }.merge().toMutableList()

        do {
            val moreEpsilons = epsilons
                    .map { it.acceptEpsilons() }.merge()
                    .filter { !epsilons.contains(it) }
            epsilons.addAll(moreEpsilons)
        } while (moreEpsilons.isNotEmpty())

        states.addAll(epsilons)
        return epsilons.isNotEmpty()
    }

    fun accept(char: Char): Boolean {
        val newStates = states.map { it.accept(char) }.merge()
        states.clear()
        states.addAll(newStates)

        return updateEpsilons() || newStates.isNotEmpty()
    }

    fun isRunning() = states.isNotEmpty()

    fun isAccepting() = states.any { it.isAccepting() }

    fun getCurrentOutput(): List<T> = states.filter { it.isAccepting() }.map { it.output!! }.distinct()

    //TODO: Temporary solution (Not very efficient, can have many duplicate states with transitions)
    fun merge(other: FSMachine<T>) = FSMachine(states + other.states) //TODO: Set accept output for each side of the machine here then set any accept states to the appropriate value.

    fun getStateCount() = states.count()
}

class RegexScanner<T>(private val regex: String) {
    private var current = 0
    private val root = State<T>()
    private var endState = root
    private var metaScope = root
    private var orScope = root
    private var scopeChange = false
    private var orState: State<T>? = null
    private var orEndState: State<T>? = null
    private var afterOr = false
    private var afterMeta = false

    private val numericalPredicate = '0' range '9' //
    private val alphaNumericPredicate = numericalPredicate or ('A' range 'Z') or ('a' range 'z')
    private val anyCharacterPredicate: (Char) -> Boolean = { true }

    companion object {
        private const val END_CHAR: Char = '\n'
        private const val metaCharacters = "*+?|"
    }

    fun toFSM(): State<T> {
        while (!atEnd()) {
            var escape = false
            val char = peek()

            if (scopeChange) {
                metaScope = endState
                scopeChange = false
            }

            if (char == '\\') {
                escape = true
                advance()
            }

            val startEnd = if (!escape) {
                when (char) {
                    '[' -> orBlock()
                    '(' -> parenthesis()
                    '{' -> TODO("Repetitions -> Relies on metaScope too") //TODO: Might not need this for my language specifically, but should implement if I want this to be a full regex parser
                    in metaCharacters -> metaCharacter()
                    else -> char()
                }
            } else {
                char(escape)
            }

            endState = startEnd.second
            endState.removeAccept()

            if (afterOr) {
                endState = startEnd.first
                orScope = endState //This is the only place it changes
                afterOr = false
            }
        }

        if (orState != null) {
            endState.addEpsilonTransition(orEndState!!)
            endState = orEndState!!
        }

        endState.forceAccept = true

        return root
    }

    fun advance(): Char {
        val char = peek()
        if (!atEnd()) current++
        return char
    }

    fun isNormalChar() = peek() !in "*+?|(){}[]\n"

    fun peek() = if (current < regex.length) regex[current] else END_CHAR

    fun atEnd() = current >= regex.length

    fun char(escape: Boolean = false): Pair<State<T>, State<T>> { //TODO: Error when escaping normal characters, also implement \w \d etc.?
        val charEnd = State<T>(forceAccept = true)
        val char = advance()

        val predicate = if (!escape) when (char) { //TODO: Find a nicer way to deal with this, including the isRange part.
            '.' -> anyCharacterPredicate //Any
            else -> {
                afterMeta = true
                char.predicate()
            }
        } else when (char) {
            'd' -> numericalPredicate
            'w' -> alphaNumericPredicate //Alphanumerical
            else -> {
                afterMeta = false
                char.predicate()
            }
        }

        endState.addTransition(predicate, charEnd)

        if (afterMeta) {
            afterMeta = false
            metaScope = endState
        }

        return endState to charEnd
    }

    fun orBlock(): Pair<State<T>, State<T>> {
        scopeChange = true
        val end = State<T>(forceAccept = true)
        var transitionPredicate: (Char) -> Boolean = { false }

        advance() //Consume '['

        do {
            val char = advance()
            transitionPredicate = if (peek() == '-') { //Is Range
                advance() //Consume '-'
                transitionPredicate or (char range advance())
            } else transitionPredicate or char.predicate()
        } while (peek() != ']' && peek() != END_CHAR)

        advance() //Consume ']'

        endState.addTransition(transitionPredicate, end)
        return endState to end
    }

    fun parenthesis(): Pair<State<T>, State<T>> {
        advance() //Consume '('
        val scanner = RegexScanner<T>(advanceUntil(')'))
        val states = scanner.toFSM()
        endState.addEpsilonTransition(states)
        metaScope = states //TODO: scopeChange = true?

        return states to scanner.endState
    }

    fun metaCharacter(): Pair<State<T>, State<T>> {
        afterMeta = true

        return when (advance()) {
            '|' -> or()
            '*' -> asterisk()
            '+' -> plus()
            '?' -> question()
            else -> TODO("Not a valid metaCharacter (Should never happen). Throw Exception, or log error")
        }
    }

    fun or(): Pair<State<T>, State<T>> {
        scopeChange = true
        afterOr = true

        if (orState == null) { //First or in regex
            orState = State()
            orEndState = State()

            val newState = orScope.transferToNext()
            orScope.replaceWith(orState!!)
            orState = orScope
            orScope.addEpsilonTransition(newState)
        } else orState!!.addEpsilonTransition(orScope)

        endState.addEpsilonTransition(orEndState!!)

        val extraState = State<T>()

        orState!!.addEpsilonTransition(extraState)

        return extraState to orEndState!!
    }

    fun asterisk(): Pair<State<T>, State<T>> {
        val newEndState = State<T>(forceAccept = true)

        endState.addEpsilonTransition(metaScope)
        metaScope.addEpsilonTransition(newEndState)

        return metaScope to newEndState
    }

    fun plus(): Pair<State<T>, State<T>> {
        val newEndState = State<T>(forceAccept = true)

        endState.addEpsilonTransition(metaScope)
        endState.addEpsilonTransition(newEndState)

        return metaScope to newEndState
    }

    fun question(): Pair<State<T>, State<T>> {
        val newEndState = State<T>(forceAccept = true)

        metaScope.addEpsilonTransition(newEndState)
        endState.addEpsilonTransition(newEndState)

        return metaScope to newEndState
    }

    private fun advanceUntil(char: Char) = advanceUntil { it == char }

    private fun advanceUntil(pred: (Char) -> Boolean): String {
        val sb = StringBuilder()

        do {
            val char = advance()
            if (!pred(char)) sb.append(char) //Ensure the final character isn't included. i.e. ')'
        } while (!pred(char) && char != END_CHAR) //TODO: Error if hits END_CHAR rather than the predicate
        //TODO: Could check atEnd() rather than char != END_CHAR

        return sb.toString()
    }
}

internal fun rangePredicate(start: Char, end: Char): (Char) -> Boolean = { it in start..end }
internal fun charPredicate(c: Char): (Char) -> Boolean = { it == c }
internal fun orPredicate(first: (Char) -> Boolean, second: (Char) -> Boolean): (Char) -> Boolean = { first(it) || second(it) }

internal infix fun ((Char) -> Boolean).or(other: (Char) -> Boolean): (Char) -> Boolean = orPredicate(this, other)
internal infix fun Char.range(other: Char) = rangePredicate(this, other)
internal fun Char.predicate() = charPredicate(this)

internal fun <T> List<List<T>>.merge() = fold(emptyList<T>()) { acc, stateList -> acc + stateList }
