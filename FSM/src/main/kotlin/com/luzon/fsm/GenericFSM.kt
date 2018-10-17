package com.luzon.fsm

import com.luzon.utils.Predicate
import com.luzon.utils.merge
import com.luzon.utils.replaceWith
import java.util.*

fun <K : Any, T : Any> Collection<OutputFSM<K, T>>.toMergedFSM() = FSM.merge(*toTypedArray())

interface FSM<Alphabet> {
    fun accept(character: Alphabet): Boolean
    fun merge(fsMachine: FSM<Alphabet>): FSM<Alphabet>
    fun copy(): FSM<Alphabet>
    fun reset()

    val isRunning: Boolean
    val isNotRunning: Boolean get() = !isRunning

    val isAccepting: Boolean
    val isNotAccepting: Boolean get() = !isAccepting

    val acceptStates: List<State<Alphabet>>

    companion object {
        fun <Output : Any> fromRegex(str: String) = OutputFSM<Char, Output>(RegexScanner<Output>(str).toFSM())

        fun <Alphabet> merge(vararg machines: FSM<Alphabet>): FSM<Alphabet> =
                machines.reduce { acc, fsMachine ->
                    acc.merge(fsMachine)
                }
    }
}

interface State<Alphabet> {
    fun accept(character: Alphabet): List<State<Alphabet>>
    val epsilons: List<State<Alphabet>>

    var accepting: Boolean
    val leaf: Boolean

    fun removeAccept()
    fun transferToNext(): State<Alphabet>
    fun replaceWith(other: State<Alphabet>)
}

open class NormalFSM<Alphabet>(protected val states: MutableList<State<Alphabet>> = mutableListOf(),
                               updateEpsilons: Boolean = true) : FSM<Alphabet> {

    protected val originalStates = states.toMutableList()

    constructor(state: State<Alphabet>, updateEpsilons: Boolean = true) : this(mutableListOf(state), updateEpsilons)

    init {
        if (updateEpsilons) updateEpsilons(true)
    }

    override fun merge(fsMachine: FSM<Alphabet>): NormalFSM<Alphabet> {
        val states = mutableListOf<State<Alphabet>>()
        states.addAll(this.states)
        if (fsMachine is NormalFSM<Alphabet>) states.addAll(fsMachine.states)

        return NormalFSM(states)
    }

    override fun copy() = NormalFSM(originalStates, false)

    override fun reset() {
        states.replaceWith(originalStates)
    }

    private fun updateEpsilons(updateOriginal: Boolean = false): Boolean {
        val epsilons = states.map { it.epsilons }.merge().toMutableList()

        do {
            val moreEpsilons = epsilons
                    .map { it.epsilons }.merge()
                    .filter { !epsilons.contains(it) }
            epsilons.addAll(moreEpsilons)
        } while (moreEpsilons.isNotEmpty())

        states.addAll(epsilons)
        if (updateOriginal) originalStates.addAll(epsilons)

        return epsilons.isNotEmpty()
    }

    override fun accept(character: Alphabet): Boolean {
        val newStates = states.map { it.accept(character) }.merge()
        states.replaceWith(newStates)

        return updateEpsilons() || newStates.isNotEmpty()
    }

    override val isRunning get() = states.isNotEmpty()
    override val isAccepting get() = states.any { it.accepting }
    override val acceptStates get() = states.filter { it.accepting }

    val stateCount: Int get() = states.size
}

open class NormalState<Alphabet>(override var accepting: Boolean = false) : State<Alphabet> {
    protected val transitions = mutableListOf<Transition<Alphabet>>()
    protected val epsilonTransitions = mutableListOf<State<Alphabet>>()

    override val leaf: Boolean get() = transitions.isEmpty() && epsilonTransitions.isEmpty()
    override val epsilons get() = epsilonTransitions

    override fun accept(character: Alphabet) = transitions.mapNotNull { it.accept(character) }

    fun addTransition(transition: Transition<Alphabet>) = transitions.add(transition)

    fun addTransition(predicate: Predicate<Alphabet>, state: State<Alphabet>) =
            addTransition(Transition(predicate, state))

    fun addEpsilonTransition(state: State<Alphabet>) = epsilonTransitions.add(state)

    override fun removeAccept() {
        accepting = false
    }

    override fun transferToNext(): NormalState<Alphabet> {
        val newState = NormalState<Alphabet>(accepting)
        newState.transitions.addAll(transitions)
        newState.epsilonTransitions.addAll(epsilonTransitions)

        transitions.clear()
        epsilonTransitions.clear()
        accepting = false

        addEpsilonTransition(newState)

        return newState
    }

    override fun replaceWith(other: State<Alphabet>) {
        if (other is NormalState<Alphabet>) {
            transitions.replaceWith(other.transitions)
            epsilonTransitions.replaceWith(other.epsilonTransitions)
            accepting = other.accepting
        }
    }

    protected fun findAllChildren(): Set<NormalState<Alphabet>> {
        val cachedStates = mutableSetOf<NormalState<Alphabet>>()
        val stateStack = Stack<NormalState<Alphabet>>()
        cachedStates.add(this)
        stateStack.push(this)

        while (stateStack.isNotEmpty()) {
            val currentState = stateStack.pop() as NormalState<Alphabet>
            currentState.transitions.forEach { (_, state) ->
                val normalState = state as NormalState<Alphabet>
                if (cachedStates.add(normalState)) stateStack.push(normalState)
            }

            currentState.epsilonTransitions.forEach { state ->
                val normalState = state as NormalState<Alphabet>
                if (cachedStates.add(normalState)) stateStack.push(normalState)
            }
        }

        return cachedStates
    }

    val leaves: List<State<Alphabet>> get() = findAllChildren().filter { it.leaf }
}

data class Transition<Alphabet>(val predicate: Predicate<Alphabet>, val state: State<Alphabet>) {
    fun accept(character: Alphabet): State<Alphabet>? = if (predicate(character)) state else null
}

@Suppress("UNCHECKED_CAST")
open class OutputFSM<Alphabet, Output : Any>(states: MutableList<State<Alphabet>> = mutableListOf(),
                                             updateEpsilons: Boolean = true) :
        NormalFSM<Alphabet>(states, updateEpsilons) {
    constructor(state: State<Alphabet>, updateEpsilons: Boolean = true) : this(mutableListOf(state), updateEpsilons)

    val isOutputting get() = states.any { (it as? OutputState<*, *>)?.isOutputting ?: false }
    val isNotOutputting get() = !isOutputting

    override val isAccepting: Boolean
        get() = super.isAccepting || isOutputting

    val currentOutput: List<Output>
        get() = states.asSequence()
                .filter { it is OutputState<Alphabet, *> && it.isOutputting }
                .mapNotNull { (it as? OutputState<Alphabet, Output>)?.output }
                .toList()

    fun replaceChildOutputs(output: Output) = apply {
        // TODO: This is being called but not calling replaceChildOutputs
        states.forEach {
            (it as? OutputState<Alphabet, Output>)?.replaceChildOutput(output)
        }
    }

    override fun copy() = OutputFSM<Alphabet, Output>(originalStates, false)

    override fun merge(fsMachine: FSM<Alphabet>): OutputFSM<Alphabet, Output> {
        val states = mutableListOf<State<Alphabet>>()
        states.addAll(this.states)
        if (fsMachine is OutputFSM<Alphabet, *>) states.addAll(fsMachine.states)

        return OutputFSM(states)
    }
}

@Suppress("UNCHECKED_CAST")
open class OutputState<Alphabet, Output>(accepting: Boolean = false,
                                         var output: Output? = null) : NormalState<Alphabet>(accepting) {
    val isOutputting get() = output != null
    val isNotOutputting get() = !isOutputting

    override fun removeAccept() {
        super.removeAccept()
        output = null
    }

    fun replaceOutput(output: Output?) {
        accepting = false
        this.output = output
    }

    fun replaceChildOutput(output: Output?) {
        findAllChildren().forEach {
            val outputState = it as? OutputState<Alphabet, Output>
            if (outputState != null && (outputState.isOutputting || outputState.accepting))
                outputState.replaceOutput(output)
        }
    }

    override fun transferToNext(): OutputState<Alphabet, Output> {
        val newState = OutputState<Alphabet, Output>(accepting, output)
        newState.transitions.addAll(transitions)
        newState.epsilonTransitions.addAll(epsilonTransitions)

        transitions.clear()
        epsilonTransitions.clear()
        accepting = false
        output = null

        addEpsilonTransition(newState)

        return newState
    }

    override fun replaceWith(other: State<Alphabet>) {
        super.replaceWith(other)

        if (other is OutputState<Alphabet, *>)
            output = other.output as? Output
    }
}