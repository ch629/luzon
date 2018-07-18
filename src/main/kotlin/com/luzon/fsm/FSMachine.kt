package com.luzon.fsm

//TODO: Helper class to deal with inputs and exiting with possible outputs at a specific location; to consume the correct input into a token.
data class FSMachine<T>(var states: List<State<T>>) {
    constructor(root: State<T>) : this(listOf(root))

    companion object {
        fun <T> fromRegex(str: String): FSMachine<T> {
            TODO()
        }
    }

    fun accept(char: Char): Boolean {
        var newStates = emptyList<State<T>>()
        states.forEach { newStates += it.accept(char) }
        states = newStates
        return newStates.isNotEmpty()
    }

    fun getCurrentOutput(): List<T> = states.filter { it.isAccepting() }.map { it.output!! }.distinct()

    //TODO: Temporary solution (Not very efficient, can have many duplicate states with transitions)
    fun merge(other: FSMachine<T>) = FSMachine(states + other.states)
}

class RegexScanner<T>(private val regex: String) { //TODO: Backslash metacharacters
    private val endChar: Char = '\n'
    private var current = 0

    fun toFSM(): State<T> {
        TODO("Scan Regex String into an FSM")
    }

    fun advance(): Char {
        val char = peek()
        if (!atEnd()) current++
        return char
    }

    fun peek() = if (regex.length > current) regex[current] else endChar

    fun atEnd() = current > regex.length

    fun orBlock(): State<T> {
        val root = State<T>()
        val end = State<T>()
        var pred: (Char) -> Boolean = { false }

        if (peek() == '[') advance() //Make sure the '[' isn't included within the range predicates

        do {
            val char = advance()
            pred = if (peek() == '-') { //Is Range
                advance() //Consume '-'
                orPredicate(pred, rangePredicate(char, advance()))
            } else orPredicate(pred, charPredicate(char))
        } while (char != ']' && char != endChar)
        advance() //Consume ']'

        root.addTransition(pred, end)
        return root
    }

    fun parenthesis() = RegexScanner<T>(advanceUntil(')')).toFSM()

    private fun advanceUntil(char: Char) = advanceUntil { it == char }

    private fun advanceUntil(pred: (Char) -> Boolean): String {
        val sb = StringBuilder()

        do {
            val char = advance()
            sb.append(char)
        } while (!pred(char) && char != endChar) //TODO: Error if hits endChar rather than the predicate
        //TODO: Could check atEnd() rather than char != endChar

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

class State<T>(val output: T? = null) {
    private var transitions = emptyList<Pair<(Char) -> Boolean, State<T>>>() //TODO: Merge transitions going between the same states, using or predicates for each transitional predicate (Possibly an optimization)

    fun accept(char: Char, containsEpsilons: Boolean = false): List<State<T>> {
        var newStates = transitions.filter { it.first(char) }.map { it.second }.toList()
        var epsilonStates = emptyList<State<T>>()
        if (containsEpsilons) {
            newStates.forEach {
                val epsilonTransitions = it.transitions.filter { it.first == epsilon }.map { it.second }

                epsilonStates += epsilonTransitions
            }
            newStates += epsilonStates
        }

        return newStates
    }

    fun mergeTransitions() { //TODO: Test
        val newTransitions = mutableListOf<Pair<(Char) -> Boolean, State<T>>>()
        val groupedTransitions = transitions.groupBy { it.second }
        groupedTransitions.entries.forEach {
            var pred: (Char) -> Boolean = { false }
            it.value.forEach { pred = orPredicate(pred, it.first) }
            newTransitions.add(pred to it.key)
        }
    }

    //TODO: DSL?
    fun addTransition(pred: (Char) -> Boolean, state: State<T>) {
        transitions += pred to state
    }

    fun addEpsilonTransition(state: State<T>) {
        transitions += epsilon to state
    }

    fun addEpsilonTransition(vararg states: State<T>) {
        states.forEach { transitions += epsilon to it }
    }

    fun isAccepting() = output != null

    fun findLeaves(): List<State<T>> { //TODO: Test
        if (transitions.isEmpty()) return listOf(this) //TODO: This is pretty inefficient as I'm creating a list for each single state but is a simple solution
        val list = mutableListOf<State<T>>()
        transitions.filter { it.second != this }.forEach { list.addAll(it.second.findLeaves()) }
        return list
    }

    fun setLeafEpsilons(endState: State<T>) {
        findLeaves().forEach { it.addEpsilonTransition(endState) }
    }

    fun setLeafEpsilons(vararg states: State<T>) {
        findLeaves().forEach { it.addEpsilonTransition(*states) }
    }
}

val epsilon: (Char) -> Boolean = { true }

fun rangePredicate(start: Char, end: Char): (Char) -> Boolean = { it in start..end }
fun charPredicate(c: Char): (Char) -> Boolean = { it == c }
fun andPredicate(first: (Char) -> Boolean, second: (Char) -> Boolean): (Char) -> Boolean = { first(it) && second(it) }
fun orPredicate(first: (Char) -> Boolean, second: (Char) -> Boolean): (Char) -> Boolean = { first(it) || second(it) }
