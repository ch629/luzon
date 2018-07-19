package com.luzon.fsm

class State<T>(val output: T? = null) {
    private var transitions = emptyList<Pair<(Char) -> Boolean, State<T>>>() //TODO: Merge transitions going between the same states, using or predicates for each transitional predicate (Possibly an optimization)
    private var epsilonTransitions = emptyList<State<T>>()

    fun acceptEpsilons() = epsilonTransitions
    fun accept(char: Char) = transitions.filter { it.first(char) }.map { it.second }.toList()

    fun mergeTransitions() { //TODO: Test
        val newTransitions = mutableListOf<Pair<(Char) -> Boolean, State<T>>>()
        val groupedTransitions = transitions.groupBy { it.second }
        epsilonTransitions = epsilonTransitions.distinct()
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
        epsilonTransitions += state
    }

    fun addEpsilonTransition(vararg states: State<T>) {
        states.forEach { addEpsilonTransition(it) }
    }

    fun isAccepting() = output != null
    fun hasEpsilonTransitions() = epsilonTransitions.isNotEmpty()
    fun hasOnlyEpsilon() = epsilonTransitions.isNotEmpty() && transitions.isEmpty()

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