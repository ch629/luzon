package com.luzon.fsm

import com.luzon.fsm.scanner.RegexScanner
import com.luzon.utils.Predicate
import java.util.*

class FSM<T, O>(private var states: List<State<T, O>>, updateEpsilons: Boolean = true) {
    private val initialStates: List<State<T, O>>

    init {
        initialStates = states + if (updateEpsilons) acceptRecursiveEpsilons(states) else emptyList()
        reset()
    }

    companion object {
        fun <O : Any> fromRegex(str: String) = FSM(listOf(RegexScanner<O>(str).toFSM()))

        fun <T, O> merge(vararg machines: FSM<T, O>): FSM<T, O> {
            val allInitialStates = mutableListOf<State<T, O>>()
            // I think this will be more efficient than merging each in a reduce
//            val c = machines.fold(mutableListOf<State<T, O>>()) { acc, machine ->
//                acc.apply { addAll(machine.initialStates) }
//            }

            machines.forEach { allInitialStates.addAll(it.initialStates) }

            return FSM(allInitialStates, false)
        }
    }

    fun merge(other: FSM<T, O>) =
            FSM(initialStates + other.initialStates, false)

    fun reset() {
        states = initialStates // TODO: Check to make sure nothing changes the initial states
    }

    fun replaceChildOutputs(value: O?) = apply {
        states.forEach { it.replaceChildOutput(value) }
    }

    fun accept(value: T): Boolean {
        val newStates = acceptNormal(value)
        newStates.addAll(acceptRecursiveEpsilons(newStates))

        states = newStates

        return running
    }

    private fun acceptRecursiveEpsilons(stateList: List<State<T, O>>): List<State<T, O>> {
        val list = mutableListOf<State<T, O>>()
        var epsilons: List<State<T, O>> = acceptEpsilons(stateList)

        while (epsilons.isNotEmpty()) {
            list.addAll(epsilons)
            epsilons = acceptEpsilons(epsilons).filter { !epsilons.contains(it) }
        }

        return list
    }

    private fun acceptEpsilons(stateList: List<State<T, O>> = states) =
            stateList.fold(mutableListOf<State<T, O>>()) { acc, states ->
                acc.apply { addAll(states.epsilonTransitions) }
            }

    private fun acceptNormal(value: T, stateList: List<State<T, O>> = states) =
            stateList.fold(mutableListOf<State<T, O>>()) { acc, states ->
                acc.apply { addAll(states.accept(value)) }
            }

    fun copyOriginal() = FSM(initialStates, false)

    val accepting: Boolean
        get() = states.any { it.accepting != null || it.forceAccept }

    val acceptValue: O?
        get() = states.firstOrNull { it.accepting != null }?.accepting

    val running: Boolean
        get() = states.isNotEmpty()

    val stateCount: Int
        get() = states.size
}

class State<T, O>(private val transitions: MutableList<Transition<T, O>> = mutableListOf(),
                  val epsilonTransitions: MutableList<State<T, O>> = mutableListOf(),
                  var accepting: O? = null, var forceAccept: Boolean = false) {
    val leaf: Boolean
        get() = transitions.isEmpty() && epsilonTransitions.isEmpty()

    fun accept(value: T): List<State<T, O>> = transitions.mapNotNull { it.accept(value) }

    fun addTransition(predicate: Predicate<T>, state: State<T, O>) =
            transitions.add(Transition(predicate, state))

    fun addEpsilon(state: State<T, O>) =
            epsilonTransitions.add(state)

    fun removeAccept() {
        accepting = null
        forceAccept = false
    }

    fun transferToNext(): State<T, O> {
        val newState = State(transitions.toMutableList(),
                epsilonTransitions.toMutableList(), accepting, forceAccept)
        transitions.clear()
        epsilonTransitions.clear()

        removeAccept()
        addEpsilon(newState)

        return newState
    }

    fun replaceWith(other: State<T, O>) {
        transitions.clear()
        transitions.addAll(other.transitions)
        epsilonTransitions.clear()
        epsilonTransitions.addAll(other.epsilonTransitions)
        accepting = other.accepting
        forceAccept = other.forceAccept
    }

    fun replaceChildOutput(output: O?) {
        findAllChildren().forEach { it.replaceOutput(output) }
    }

    private fun replaceOutput(output: O?) {
        if (accepting != null || forceAccept) {
            accepting = output
            forceAccept = false
        }
    }

    private fun findAllChildren(): Set<State<T, O>> {
        val cachedStates = mutableSetOf<State<T, O>>()
        val stateStack = Stack<State<T, O>>()
        cachedStates.add(this)
        stateStack.push(this)

        while (stateStack.isNotEmpty()) {
            val currentState = stateStack.pop()
            currentState.transitions.forEach { (_, state) ->
                if (cachedStates.add(state))
                    stateStack.push(state)
            }

            currentState.epsilonTransitions.forEach { state ->
                if (cachedStates.add(state))
                    stateStack.push(state)
            }
        }

        return cachedStates
    }
}

data class Transition<T, O>(val predicate: Predicate<T>, val state: State<T, O>) {
    fun accept(value: T) = if (predicate(value)) state else null
}

fun <A : Any, O : Any> Collection<FSM<A, O>>.toMergedFSM() = FSM.merge(*toTypedArray())