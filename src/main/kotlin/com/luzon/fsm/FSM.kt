package com.luzon.fsm

import com.luzon.utils.merge

class FSM<Alphabet, Output>(statesList: List<State<Alphabet, Output>> = emptyList(), updateEpsilons: Boolean = true) {
    constructor(root: State<Alphabet, Output>, updateEpsilons: Boolean = true) : this(mutableListOf(root), updateEpsilons)

    val states = statesList.toMutableList()
    private val originalStates = statesList.toMutableList()

    init {
        if (updateEpsilons) updateEpsilons(true)
    }

    companion object {
        fun <Output> fromRegex(str: String) = FSM(RegexScanner<Output>(str).toFSM())

        fun <Alphabet, Output> merge(vararg machines: FSM<Alphabet, Output>): FSM<Alphabet, Output> =
                machines.reduce { acc, fsMachine ->
                    acc.merge(fsMachine)
                }
    }

    fun copy() = FSM(originalStates, false)

    private fun updateEpsilons(updateOriginal: Boolean = false): Boolean {
        val epsilons = states.map { it.acceptEpsilons() }.merge().toMutableList()

        do {
            val moreEpsilons = epsilons
                    .map { it.acceptEpsilons() }.merge()
                    .filter { !epsilons.contains(it) }
            epsilons.addAll(moreEpsilons)
        } while (moreEpsilons.isNotEmpty())

        states.addAll(epsilons)
        if (updateOriginal) originalStates.addAll(epsilons)

        onEnter(epsilons)

        return epsilons.isNotEmpty()
    }

    fun accept(value: Alphabet): Boolean {
        val newStates = states.map { it.accept(value) }.merge()
        states.clear()
        states.addAll(newStates)

        onEnter(newStates)

        return updateEpsilons() || newStates.isNotEmpty()
    }

    private fun onEnter(states: Collection<State<Alphabet, Output>>) {
        states.forEach { it.onEnter() }
    }

    fun isRunning() = states.isNotEmpty()
    fun isNotRunning() = !isRunning()

    fun isAccepting() = states.any { it.isAccepting() }
    fun isNotAccepting() = !isAccepting()

    fun acceptingStates() = states.filter { it.isAccepting() }

    fun getCurrentOutput(): List<Output> = acceptingStates().filter { !it.forceAccept }.map { it.output!! }.distinct()

    //TODO: Temporary solution (Not very efficient, can have many duplicate states with transitions)
    fun merge(other: FSM<Alphabet, Output>) = FSM(originalStates + other.originalStates, false)

    fun mergeWithOutput(thisOutput: Output, other: FSM<Alphabet, Output>, otherOutput: Output): FSM<Alphabet, Output> {
        setOutput(thisOutput)
        other.setOutput(otherOutput)

        return merge(other)
    }

    fun setOutput(output: Output) = apply {
        states.forEach {
            it.replaceChildOutput(output)
        }
    }

    fun getStateCount() = states.count()

    fun reset() { //Resets machine to it's original state.
        states.clear()
        states.addAll(originalStates)
    }
}
