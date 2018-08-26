package com.luzon.fsm

import com.luzon.utils.merge

class FSMachine<Alphabet, Output>(statesList: List<State<Alphabet, Output>>, updateEpsilons: Boolean = true) {
    constructor(root: State<Alphabet, Output>, updateEpsilons: Boolean = true) : this(mutableListOf(root), updateEpsilons)

    private val states = statesList.toMutableList()
    private val originalStates = statesList.toMutableList()

    init {
        if (updateEpsilons) updateEpsilons(true)
    }

    companion object {
        fun <Output> fromRegex(str: String) = FSMachine(RegexScanner<Output>(str).toFSM())

        fun <Alphabet, Output> merge(vararg machines: FSMachine<Alphabet, Output>): FSMachine<Alphabet, Output> =
                machines.reduce { acc, fsMachine ->
                    acc.merge(fsMachine)
                }
    }

    fun copy() = FSMachine(originalStates, false)

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

        return epsilons.isNotEmpty()
    }

    fun accept(value: Alphabet): Boolean {
        val newStates = states.map { it.accept(value) }.merge()
        states.clear()
        states.addAll(newStates)

        return updateEpsilons() || newStates.isNotEmpty()
    }

    fun isRunning() = states.isNotEmpty()

    fun isAccepting() = states.any { it.isAccepting() }

    fun acceptingStates() = states.filter { it.isAccepting() }

    fun getCurrentOutput(): List<Output> = acceptingStates().filter { !it.forceAccept }.map { it.output!! }.distinct()

    //TODO: Temporary solution (Not very efficient, can have many duplicate states with transitions)
    fun merge(other: FSMachine<Alphabet, Output>) = FSMachine(originalStates + other.originalStates, false)

    fun mergeWithOutput(thisOutput: Output, other: FSMachine<Alphabet, Output>, otherOutput: Output): FSMachine<Alphabet, Output> {
        setOutput(thisOutput)
        other.setOutput(otherOutput)

        return merge(other)
    }

    fun setOutput(output: Output): FSMachine<Alphabet, Output> {
        states.forEach {
            it.replaceChildOutput(output)
        }

        return this
    }

    fun getStateCount() = states.count()

    fun reset() { //Resets machine to it's original state.
        states.clear()
        states.addAll(originalStates)
    }
}
