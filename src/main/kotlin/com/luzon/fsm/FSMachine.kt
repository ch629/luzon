package com.luzon.fsm

//TODO: Helper class to deal with inputs and exiting with possible outputs at a specific location; to consume the correct input into a token.
class FSMachine<T>(statesList: List<State<T>>) {
    constructor(root: State<T>) : this(mutableListOf(root))

    private val states = statesList.toMutableList()

    companion object {
        fun <T> fromRegex(str: String) = FSMachine(RegexScanner<T>(str).toFSM())
    }

    fun accept(char: Char): Boolean {
        val epsilons = states.map { it.acceptEpsilons() }.merge() //TODO: Figure out how to deal with epsilons properly, kind of need to add epsilons recursively
        states.addAll(epsilons)
        val newStates = states.map { it.accept(char) }.merge()
        states.clear()
        states.addAll(newStates)

        return newStates.isNotEmpty()
    }

    fun isRunning() = states.isNotEmpty()

    fun getCurrentOutput(): List<T> = states.filter { it.isAccepting() }.map { it.output!! }.distinct()

    //TODO: Temporary solution (Not very efficient, can have many duplicate states with transitions)
    fun merge(other: FSMachine<T>) = FSMachine(states + other.states)

    fun getStateCount() = states.count()
}

class RegexScanner<T>(private val regex: String) { //TODO: Backslash metacharacters
    private var current = 0

    companion object {
        private const val END_CHAR: Char = '\n'
    }

    fun toFSM(): State<T> {
        val root = State<T>()
        var endStates = listOf(root)
        while (!atEnd()) {
            val char = peek()

            val sectionRoot = when (char) {
                '[' -> orBlock()
                else -> char()
            }

            endStates.forEach { it.addEpsilonTransition(sectionRoot) }
            endStates = sectionRoot.findLeaves()
        }

        return root
    }

    fun advance(): Char {
        val char = peek()
        if (!atEnd()) current++
        return char
    }

    fun peek() = if (current in 0 until regex.length) regex[current] else END_CHAR //In case backtracking is needed

    fun atEnd() = current > regex.length

    fun char(): State<T> {
        val start = State<T>()
        start.addTransition(charPredicate(advance()), State())
        return start
    }

    fun orBlock(): State<T> {
        val root = State<T>()
        val end = State<T>()
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

        root.addTransition(transitionPredicate, end)
        return root
    }

    fun parenthesis() = RegexScanner<T>(advanceUntil(')')).toFSM()

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

//TODO: This might just be included within the RegexScanner, as should be the only time it's all used.
class RegexStateHelper<T> {
    //TODO: Check all these are correct with the new metaChar stuff

    fun asterix(inner: State<T>) = metaChar(inner, rootEpsilonEnd = true)
    fun plus(inner: State<T>) = metaChar(inner, rootLeafRoot = true)
    fun question(inner: State<T>) = metaChar(inner, rootEpsilonEnd = true)
    fun or(left: State<T>, right: State<T>) = metaChar(left, right)

    private fun metaChar(vararg rootEpsilon: State<T>) = metaChar(rootEpsilon.asList())

    private fun metaChar(rootEpsilon: State<T>, rootEpsilonEnd: Boolean = false,
                         leafEpsilon: List<State<T>> = emptyList(), rootLeafRoot: Boolean = false) =
            metaChar(listOf(rootEpsilon), rootEpsilonEnd, leafEpsilon, rootLeafRoot)

    private fun metaChar(rootEpsilon: List<State<T>>, rootEpsilonEnd: Boolean = false,
                         leafEpsilon: List<State<T>> = emptyList(), rootLeafRoot: Boolean = false): State<T> {
        val root = State<T>()
        val endState = State<T>()
        val leafEpsilons = leafEpsilon + endState

        if (rootEpsilonEnd) root.addEpsilonTransition(*(rootEpsilon + endState).toTypedArray())
        else root.addEpsilonTransition(*rootEpsilon.toTypedArray())

        if (rootLeafRoot) root.setLeafEpsilons(*(leafEpsilon + root).toTypedArray())
        else root.setLeafEpsilons(*leafEpsilons.toTypedArray())

        return root
    }
}

val epsilon: (Char) -> Boolean = { true }

fun rangePredicate(start: Char, end: Char): (Char) -> Boolean = { it in start..end }
fun charPredicate(c: Char): (Char) -> Boolean = { it == c }
fun andPredicate(first: (Char) -> Boolean, second: (Char) -> Boolean): (Char) -> Boolean = { first(it) && second(it) }
fun orPredicate(first: (Char) -> Boolean, second: (Char) -> Boolean): (Char) -> Boolean = { first(it) || second(it) }

fun <T> List<List<T>>.merge() = fold(emptyList<T>()) { acc, stateList -> acc + stateList }
