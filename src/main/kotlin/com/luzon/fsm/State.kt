package com.luzon.fsm

import com.luzon.utils.orPredicate
import java.util.*

class State<Alphabet, Output>(var output: Output? = null, var forceAccept: Boolean = false) {
    private val transitions = mutableListOf<Transition<Alphabet, Output>>()
    private val epsilonTransitions = mutableListOf<State<Alphabet, Output>>()

    private data class Transition<Alphabet, Output>(val predicate: (Alphabet) -> Boolean,
                                                    val state: State<Alphabet, Output>) {
        fun accepts(value: Alphabet) = predicate(value)
    }

    fun acceptEpsilons() = epsilonTransitions
    fun accept(value: Alphabet) = transitions.filter { it.accepts(value) }.map { it.state }
    fun isLeaf() = transitions.isEmpty() && epsilonTransitions.isEmpty()

    //TODO: Test
    fun mergeTransitions() = apply {
        val groupedTransitions = transitions.groupBy { it.state }
        val newEpsilonTransitions = epsilonTransitions.distinct()

        val newTransitions = groupedTransitions.entries.map { (state, list) ->
            Transition(orPredicate(*list.map { it.predicate }.toTypedArray()), state)
        }

        epsilonTransitions.clear()
        epsilonTransitions.addAll(newEpsilonTransitions)
        transitions.clear()
        transitions.addAll(newTransitions)
    }

    //TODO: DSL?
    fun addTransition(predicate: (Alphabet) -> Boolean, state: State<Alphabet, Output>) = apply {
        transitions.add(Transition(predicate, state))
    }

    fun addEpsilonTransition(state: State<Alphabet, Output>) = apply {
        if (state != this && !epsilonTransitions.contains(state)) epsilonTransitions.add(state)
    }

    fun addEpsilonTransition(vararg states: State<Alphabet, Output>) = apply {
        states.forEach { addEpsilonTransition(it) }
    }

    fun isAccepting() = forceAccept || output != null
    fun findLeaves() = findAllChildren().filter { it.transitions.isEmpty() && it.epsilonTransitions.isEmpty() }
    fun replaceChildOutput(output: Output) = findAllChildren().filter { it.isAccepting() }.forEach {
        it.forceAccept = false
        it.output = output
    }

    fun transferTo(other: State<Alphabet, Output>) = apply {
        //Transfers all the transitional data to another state
        other.epsilonTransitions.addAll(epsilonTransitions)
        other.transitions.addAll(transitions)
        other.output = output
        other.forceAccept = forceAccept

        epsilonTransitions.clear()
        transitions.clear()
        output = null
        forceAccept = false
    }

    fun transferToNext(): State<Alphabet, Output> {
        val newState = State<Alphabet, Output>()
        transferTo(newState)
        addEpsilonTransition(newState)
        return newState
    }

    fun replaceWith(other: State<Alphabet, Output>) = apply {
        epsilonTransitions.clear()
        transitions.clear()

        epsilonTransitions.addAll(other.epsilonTransitions)
        transitions.addAll(other.transitions)
        output = other.output
        forceAccept = other.forceAccept
    }

    private fun findAllChildren(): Set<State<Alphabet, Output>> {
        val cachedStates = mutableSetOf<State<Alphabet, Output>>()
        val stateStack = Stack<State<Alphabet, Output>>()
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

    fun removeAccept() = apply {
        forceAccept = false
        output = null
    }

    fun addFSMStates(fsm: FSM<Alphabet, Output>) {
        fsm.states.forEach { addEpsilonTransition(it) }
    }
}