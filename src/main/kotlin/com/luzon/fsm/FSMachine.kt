package com.luzon.fsm

data class FSMachine<T>(var states: List<State<T>>) {
    constructor(root: State<T>) : this(listOf(root))

    companion object {
        fun <T> fromRegex(str: String): FSMachine<T> {
            TODO()
        }
    }

    fun accept(char: Char) {
        var newStates = emptyList<State<T>>()
        states.forEach { newStates += it.accept(char) }
        states = newStates
    }

    fun merge(other: FSMachine<T>): FSMachine<T> { //TODO: Merge multiple Regex to the correct output letter
        TODO("Merge two FSM together, merging the same regex to lead to an end result (Good for numerical literals, because the only difference between a double and a float is the f on the end.")
    }
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

    fun addTransition(pred: (Char) -> Boolean, state: State<T>) {
        transitions += pred to state
    }

    fun addEpsilonTransition(state: State<T>) {
        transitions += epsilon to state
    }

    fun isAccepting() = output != null

    //TODO: onEnter, onLeave etc? -> Might not use this really.
}

val epsilon: (Char) -> Boolean = { true }