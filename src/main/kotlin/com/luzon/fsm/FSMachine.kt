package com.luzon.fsm

data class FSMachine(var states: List<State>) {
    constructor(root: State) : this(listOf(root))

    companion object {
        fun fromRegex(str: String): FSMachine {
            TODO()
        }
    }

    fun accept(char: Char): List<State> {
        TODO()
    }
}

data class State(val accept: Boolean = false) {
    val transitions = mapOf<Regex, State>()

    fun accept(char: Char) = transitions.entries.filter { it.key.matches(char.toString()) }.map { it.value }.toList()

    //TODO: onEnter, onLeave etc
}