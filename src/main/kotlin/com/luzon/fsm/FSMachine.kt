package com.luzon.fsm

import com.luzon.utils.merge

class FSMachine<T>(statesList: List<State<T>>) {
    constructor(root: State<T>) : this(mutableListOf(root))

    private val states = statesList.toMutableList()
    private val originalStates = statesList

    init {
        updateEpsilons()
    }

    companion object {
        fun <T> fromRegex(str: String) = FSMachine(RegexScanner<T>(str).toFSM())

        fun <T> merge(vararg machines: FSMachine<T>): FSMachine<T> = machines.reduce { acc, fsMachine ->
            acc.merge(fsMachine)
        }
    }

    private fun updateEpsilons(): Boolean {
        val epsilons = states.map { it.acceptEpsilons() }.merge().toMutableList()

        do {
            val moreEpsilons = epsilons
                    .map { it.acceptEpsilons() }.merge()
                    .filter { !epsilons.contains(it) }
            epsilons.addAll(moreEpsilons)
        } while (moreEpsilons.isNotEmpty())

        states.addAll(epsilons)
        return epsilons.isNotEmpty()
    }

    fun accept(char: Char): Boolean {
        val newStates = states.map { it.accept(char) }.merge()
        states.clear()
        states.addAll(newStates)

        return updateEpsilons() || newStates.isNotEmpty()
    }

    fun isRunning() = states.isNotEmpty()

    fun isAccepting() = states.any { it.isAccepting() }

    fun acceptingStates() = states.filter { it.isAccepting() }

    fun getCurrentOutput(): List<T> = acceptingStates().filter { !it.forceAccept }.map { it.output!! }.distinct()

    //TODO: Temporary solution (Not very efficient, can have many duplicate states with transitions)
    fun merge(other: FSMachine<T>) = FSMachine(states + other.states)

    fun mergeWithOutput(thisOutput: T, other: FSMachine<T>, otherOutput: T): FSMachine<T> {
        setOutput(thisOutput)
        other.setOutput(otherOutput)

        return merge(other)
    }

    fun setOutput(output: T): FSMachine<T> {
        states.forEach {
            it.replaceChildOutput(output)
        }

        return this
    }

    fun getStateCount() = states.count()

    fun reset() { //Resets machine to it's original state.
        states.clear()
        states.addAll(originalStates)

        updateEpsilons()
    }
}
