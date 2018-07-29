package com.luzon.fsm

//TODO: Helper class to deal with inputs and exiting with possible outputs at a specific location; to consume the correct input into a token.
class FSMachine<T>(statesList: List<State<T>>) {
    constructor(root: State<T>) : this(mutableListOf(root))

    private val states = statesList.toMutableList()

    companion object {
        fun <T> fromRegex(str: String) = FSMachine(RegexScanner<T>(str).toFSM())
    }

    fun accept(char: Char): Boolean {
        val epsilons = states.map { it.acceptEpsilons() }.merge().toMutableList()

        do {
            val moreEpsilons = epsilons
                    .filter { !epsilons.contains(it) && it.hasEpsilonTransitions() }
                    .map { it.acceptEpsilons() }.merge()
            epsilons.addAll(moreEpsilons)
        } while (moreEpsilons.isNotEmpty())

        states.addAll(epsilons)
        val newStates = states.map { it.accept(char) }.merge()
        states.clear()
        states.addAll(newStates)

        return newStates.isNotEmpty()
    }

    fun isRunning() = states.isNotEmpty()

    fun getCurrentOutput(): List<T> = states.filter { it.isAccepting() }.map { it.output!! }.distinct()

    //TODO: Temporary solution (Not very efficient, can have many duplicate states with transitions)
    fun merge(other: FSMachine<T>) = FSMachine(states + other.states) //TODO: Set accept output for each side of the machine here then set any accept states to the appropriate value.

    fun getStateCount() = states.count()
}

class RegexScanner<T>(private val regex: String) { //TODO: Backslash metacharacters
    private var current = 0
    private val meta = RegexMetaCharHelper<T>()
    private val root = State<T>()
    private var endState = root
    private var metaScope = root
    private var scopeChange = false
    private var orState: State<T>? = null
    private var orEndState: State<T>? = null
    private var afterOr = false

    companion object {
        private const val END_CHAR: Char = '\n'
        private const val metaCharacters = "*+?|"
    }

    private class RegexMetaCharHelper<T> {
        fun plus(inner: State<T>) = metaChar(inner, rootLeafRoot = true)
        fun question(inner: State<T>) = metaChar(inner, rootEpsilonEnd = true)

        private fun metaChar(rootEpsilon: State<T>, rootEpsilonEnd: Boolean = false,
                             leafEpsilon: List<State<T>> = emptyList(), rootLeafRoot: Boolean = false) =
                metaChar(listOf(rootEpsilon), rootEpsilonEnd, leafEpsilon, rootLeafRoot)

        private fun metaChar(rootEpsilon: List<State<T>>, rootEpsilonEnd: Boolean = false,
                             leafEpsilon: List<State<T>> = emptyList(), rootLeafRoot: Boolean = false): State<T> {
            val root = State<T>()
            val endState = State<T>(forceAccept = true)
            val leafEpsilons = leafEpsilon + endState

            root.addEpsilonTransition(*rootEpsilon.toTypedArray())
            if (rootEpsilonEnd) root.addEpsilonTransition(endState)

            root.addLeafEpsilons(*leafEpsilons.toTypedArray())
            if (rootLeafRoot) root.addLeafEpsilons(root)

            return root
        }

        fun asterisk(scope: State<T>, endState: State<T>): State<T> {
            val newEndState = State<T>(forceAccept = true)

            endState.addEpsilonTransition(scope)
            scope.addEpsilonTransition(newEndState)

            return scope
        }
    }

    fun toFSM(): State<T> {
        while (!atEnd()) {
            val char = peek()

            val start = when (char) {
                '[' -> orBlock()
                '(' -> parenthesis()
                '{' -> TODO("Repetitions -> Relies on metaScope too") //TODO: Might not need this for my language specifically, but should implement if I want this to be a full regex parser
                in metaCharacters -> metaCharacter()
                else -> char()
            }

            val accept = endState.findAcceptChildren()
            endState = if (accept.isNotEmpty()) accept[0] else start.findLeaves()[0] //TODO: Make sure everything either returns with an accepting state or a single leaf.
            endState.removeAccept()

            if (afterOr) {
                endState.addEpsilonTransition(orEndState!!)
                afterOr = false
            }

            if (scopeChange) {
                metaScope = endState
                scopeChange = false
            }
        }

        endState.forceAccept = true //Mainly for parenthesis

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

    fun char(): State<T> {
        val charEnd = State<T>(forceAccept = true)
        endState.addTransition(charPredicate(advance()), charEnd)
        return endState
    }

    fun orBlock(): State<T> { //TODO: Potentially return a Pair<State<T>, State<T>> containing both first and last states -> Maybe make a data class to make it easier to understand
        val end = State<T>(forceAccept = true)
        var transitionPredicate: (Char) -> Boolean = { false }

        advance() //Consume '['

        do {
            val char = advance()
            transitionPredicate = if (peek() == '-') { //Is Range
                advance() //Consume '-'
                orPredicate(transitionPredicate, rangePredicate(char, advance()))
            } else orPredicate(transitionPredicate, charPredicate(char))
        } while (char != ']' && char != END_CHAR)

        advance() //Consume ']'

        endState.addTransition(transitionPredicate, end)
        return endState
    }

    fun parenthesis(): State<T> {
        advance() //Consume '('
        val states = RegexScanner<T>(advanceUntil(')')).toFSM()
        endState.addEpsilonTransition(states)
        metaScope = states //TODO: scopeChange = true?

        return states
    }

    fun metaCharacter(): State<T> = when (advance()) {
        '|' -> {
            scopeChange = true
            if (orState == null) { //First or in the regex
                val or = State<T>()
                orState = or
                root.addEpsilonTransition(orState!!)
            }

            orState!!.addEpsilonTransition(metaScope)

            if (orState == null) { //First or
                orState = State()
                orEndState = State()
                orState!!.addEpsilonTransition(metaScope)
                metaScope.findLeaves()[0].addEpsilonTransition(orEndState!!)
            }

            afterOr = true
            orState!!
        }
        '*' -> meta.asterisk(metaScope, endState)
        '+' -> meta.plus(metaScope)
        '?' -> meta.question(metaScope) //TODO: Some of these may need to change the metaScope
        else -> TODO("Not a valid metaCharacter (Should never happen). Throw Exception, or log error")
    }

    private fun advanceUntil(char: Char) = advanceUntil { it == char }

    private fun advanceUntil(pred: (Char) -> Boolean): String {
        val sb = StringBuilder()

        do {
            val char = advance()
            sb.append(char)
        } while (!pred(char) && char != END_CHAR) //TODO: Error if hits END_CHAR rather than the predicate
        //TODO: Could check atEnd() rather than char != END_CHAR

        return sb.toString()
    }
}

fun rangePredicate(start: Char, end: Char): (Char) -> Boolean = { it in start..end }
fun charPredicate(c: Char): (Char) -> Boolean = { it == c }
fun orPredicate(first: (Char) -> Boolean, second: (Char) -> Boolean): (Char) -> Boolean = { first(it) || second(it) }

fun <T> List<List<T>>.merge() = fold(emptyList<T>()) { acc, stateList -> acc + stateList }
