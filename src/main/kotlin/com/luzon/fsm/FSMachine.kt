package com.luzon.fsm

class FSMachine<T>(statesList: List<State<T>>) {
    constructor(root: State<T>) : this(mutableListOf(root))

    private val states = statesList.toMutableList()
    private val originalStates = statesList

    init {
        updateEpsilons()
    }

    companion object {
        fun <T> fromRegex(str: String) = FSMachine(RegexScanner<T>(str).toFSM())
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

    fun getCurrentOutput(): List<T> = states.filter { it.isAccepting() }.map { it.output!! }.distinct()

    //TODO: Temporary solution (Not very efficient, can have many duplicate states with transitions)
    fun merge(other: FSMachine<T>) = FSMachine(states + other.states)

    fun mergeWithOutput(thisOutput: T, other: FSMachine<T>, otherOutput: T): FSMachine<T> {
        setOutput(thisOutput)
        other.setOutput(otherOutput)

        return merge(other)
    }

    private fun setOutput(output: T) {
        acceptingStates().forEach {
            it.output = output
            it.forceAccept = false
        }
    }

    fun getStateCount() = states.count()

    fun reset() { //Resets machine to it's original state.
        states.clear()
        states.addAll(originalStates)

        updateEpsilons()
    }
}

internal fun <T> List<List<T>>.merge(): List<T> = fold(mutableListOf()) { acc, stateList ->
    acc.addAll(stateList)
    acc
}
