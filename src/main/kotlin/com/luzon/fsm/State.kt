package com.luzon.fsm

import java.util.*

class State<T>(var output: T? = null, var forceAccept: Boolean = false) {
    private var transitions = mutableListOf<Pair<(Char) -> Boolean, State<T>>>() //TODO: Merge transitions going between the same states, using or predicates for each transitional predicate (Possibly an optimization)
    private var epsilonTransitions = mutableListOf<State<T>>()

    fun acceptEpsilons() = epsilonTransitions
    fun accept(char: Char) = transitions.filter { it.first(char) }.map { it.second }.toList()

    fun mergeTransitions() { //TODO: Test
        val newTransitions = mutableListOf<Pair<(Char) -> Boolean, State<T>>>()
        val groupedTransitions = transitions.groupBy { it.second }
        epsilonTransitions = epsilonTransitions.distinct().toMutableList()
        groupedTransitions.entries.forEach {
            var pred: (Char) -> Boolean = { false }
            it.value.forEach { pred = orPredicate(pred, it.first) }
            newTransitions.add(pred to it.key)
        }
    }

    //TODO: DSL?
    fun addTransition(pred: (Char) -> Boolean, state: State<T>) {
        transitions.add(pred to state)
    }

    fun addEpsilonTransition(state: State<T>) {
        epsilonTransitions.add(state)
    }

    fun addEpsilonTransition(vararg states: State<T>) {
        states.forEach { addEpsilonTransition(it) }
    }

    fun isAccepting() = forceAccept || output != null
    fun hasEpsilonTransitions() = epsilonTransitions.isNotEmpty()
    fun hasOnlyEpsilon() = epsilonTransitions.isNotEmpty() && transitions.isEmpty()
    fun findLeaves() = findAllChildren().filter { it.transitions.isEmpty() && it.epsilonTransitions.isEmpty() }
    fun findAcceptChildren() = findAllChildren().filter { it.isAccepting() }

    private fun findAllChildren(): Set<State<T>> {
        val cachedStates = mutableSetOf<State<T>>()
        val stateStack = Stack<State<T>>()
        cachedStates.add(this)
        stateStack.push(this)

        while (stateStack.isNotEmpty()) {
            val currentState = stateStack.pop()
            currentState.transitions.forEach { (_, state) ->
                if (cachedStates.add(state)) stateStack.push(state)
            }

            currentState.epsilonTransitions.forEach { state ->
                if (cachedStates.add(state)) stateStack.push(state)
            }
        }

        return cachedStates
    }

    fun addLeafEpsilons(endState: State<T>) {
        findLeaves().forEach { it.addEpsilonTransition(endState) }
    }

    fun addLeafEpsilons(vararg states: State<T>) {
        findLeaves().forEach { it.addEpsilonTransition(*states) }
    }

    fun removeAccept() {
        forceAccept = false
        output = null
    }
}