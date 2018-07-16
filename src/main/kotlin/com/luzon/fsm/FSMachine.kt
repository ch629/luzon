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

    fun merge(other: FSMachine<T>): FSMachine<T> {
        return FSMachine(states + other.states) //TODO: Temporary solution (Not very efficient, can have many duplicate states with transitions)
    }
}

class RegexStateHelper<T> {
    fun asterix(inner: State<T>): State<T> {
        val root = state()
        val endState = state()
        root.addEpsilonTransition(inner, endState)
        inner.setLeafEpsilons(root)
        return root
    }

    fun plus(inner: State<T>): State<T> {
        val root = state()
        val endState = state()
        inner.setLeafEpsilons(endState, root)
        root.addEpsilonTransition(inner)
        return root
    }

    fun question(inner: State<T>): State<T> {
        val root = state()
        val endState = state()
        root.addEpsilonTransition(inner, endState)
        inner.setLeafEpsilons(endState)
        return root
    }

    fun or(left: State<T>, right: State<T>): State<T> {
        val root = state()
        val endState = state()
        root.addEpsilonTransition(left, right)
        left.setLeafEpsilons(endState)
        return root
    }

    private fun state() = State<T>()
}

class State<T>(val output: T? = null) {
    private var transitions = emptyList<Pair<(Char) -> Boolean, State<T>>>()

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

    fun findLeaves(): List<State<T>> {
        TODO()
    }

    fun setLeafEpsilons(endState: State<T>) {
        findLeaves().forEach { it.addEpsilonTransition(endState) }
    }

    fun setLeafEpsilons(vararg states: State<T>) {
        findLeaves().forEach { it.addEpsilonTransition(*states) }
    }
}

val epsilon: (Char) -> Boolean = { true }

fun rangePredicate(start: Char, end: Char): (Char) -> Boolean = { it in end..start }
fun charPredicate(c: Char): (Char) -> Boolean = { it == c }
fun andPredicate(first: (Char) -> Boolean, second: (Char) -> Boolean): (Char) -> Boolean = { first(it) && second(it) }
fun orPredicate(first: (Char) -> Boolean, second: (Char) -> Boolean): (Char) -> Boolean = { first(it) || second(it) }
