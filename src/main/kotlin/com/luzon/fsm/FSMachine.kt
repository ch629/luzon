package com.luzon.fsm

data class FSMachine(var states: List<State>) { //TODO: Output Alphabet
    constructor(root: State) : this(listOf(root))

    companion object {
        fun fromRegex(str: String): FSMachine {
            TODO()
        }
    }

    fun accept(char: Char) {
        var newStates = emptyList<State>()
        states.forEach { newStates += it.accept(char) }
        states = newStates
    }

    fun merge(other: FSMachine): FSMachine { //TODO: Merge multiple Regex to the correct output letter
        TODO("Merge two FSM together, merging the same regex to lead to an end result (Good for numerical literals, because the only difference between a double and a float is the f on the end.")
    }
}

data class State(val accept: Boolean = false) {
    private val transitions = mapOf<(Char) -> Boolean, State>()

    fun accept(char: Char, containsEpsilons: Boolean = false): List<State> {
        var newStates = transitions.entries.filter { it.key(char) }.map { it.value }.toList()
        var epsilonStates = emptyList<State>()
        if (containsEpsilons) {
            newStates.forEach {
                val epsilonTransitions = it.transitions.filterKeys { it == epsilon }.map { it.value }

                if (epsilonTransitions.isNotEmpty())
                    epsilonStates += epsilonTransitions
            }
            newStates += epsilonStates
        }

        return newStates
    }

    //TODO: onEnter, onLeave etc
}

val epsilon: (Char) -> Boolean = {
    true
}