package com.luzon.fsm

import com.luzon.utils.orPredicate
import java.util.*

class State<T>(var output: T? = null, var forceAccept: Boolean = false) {
    private val transitions = mutableListOf<Pair<(Char) -> Boolean, State<T>>>()
    private val epsilonTransitions = mutableListOf<State<T>>()

    fun acceptEpsilons() = epsilonTransitions
    fun accept(char: Char) = transitions.filter { it.first(char) }.map { it.second }
    fun isLeaf() = transitions.isEmpty() && epsilonTransitions.isEmpty()

    fun mergeTransitions() { //TODO: Test
        val groupedTransitions = transitions.groupBy { it.second }
        val newEpsilonTransitions = epsilonTransitions.distinct()

        val newTransitions = groupedTransitions.entries.map { (state, list) ->
            orPredicate(*list.map { it.first }.toTypedArray()) to state
        }

        epsilonTransitions.clear()
        epsilonTransitions.addAll(newEpsilonTransitions)
        transitions.clear()
        transitions.addAll(newTransitions)
    }

    //TODO: DSL?
    fun addTransition(pred: (Char) -> Boolean, state: State<T>) {
        transitions.add(pred to state)
    }

    fun addEpsilonTransition(state: State<T>) {
        if (state != this && !epsilonTransitions.contains(state)) epsilonTransitions.add(state)
    }

    fun addEpsilonTransition(vararg states: State<T>) {
        states.forEach { addEpsilonTransition(it) }
    }

    fun isAccepting() = forceAccept || output != null
    fun findLeaves() = findAllChildren().filter { it.transitions.isEmpty() && it.epsilonTransitions.isEmpty() }
    fun replaceChildOutput(output: T) = findAllChildren().filter { it.isAccepting() }.forEach {
        it.forceAccept = false
        it.output = output
    }

    fun transferTo(other: State<T>) { //Transfers all the transitional data to another state
        other.epsilonTransitions.addAll(epsilonTransitions)
        other.transitions.addAll(transitions)
        other.output = output
        other.forceAccept = forceAccept

        epsilonTransitions.clear()
        transitions.clear()
        output = null
        forceAccept = false
    }

    fun transferToNext(): State<T> {
        val newState = State<T>()
        transferTo(newState)
        addEpsilonTransition(newState)
        return newState
    }

    fun replaceWith(other: State<T>) {
        epsilonTransitions.clear()
        transitions.clear()

        epsilonTransitions.addAll(other.epsilonTransitions)
        transitions.addAll(other.transitions)
        output = other.output
        forceAccept = other.forceAccept
    }

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

    fun removeAccept() {
        forceAccept = false
        output = null
    }
}