package com.luzon.fsm

@Suppress("UNCHECKED_CAST")
open class OutputFSM<A, O : Any>(states: MutableList<IState<A>> = mutableListOf(),
                                 updateEpsilons: Boolean = true) :
        FSM<A>(states, updateEpsilons) {
    constructor(state: IState<A>, updateEpsilons: Boolean = true) : this(mutableListOf(state), updateEpsilons)

    val isOutputting get() = states.any { (it as? OutputState<*, *>)?.isOutputting ?: false }
    val isNotOutputting get() = !isOutputting

    override val isAccepting: Boolean
        get() = super.isAccepting || isOutputting

    val currentOutput: List<O>
        get() = states.asSequence()
                .filter { it is OutputState<A, *> && it.isOutputting }
                .mapNotNull { (it as? OutputState<A, O>)?.output }
                .toList()

    fun replaceChildOutputs(output: O) = apply {
        // TODO: This is being called but not calling replaceChildOutputs -> Check this is happening, might be outdated.
        states.forEach {
            (it as? OutputState<A, O>)?.replaceChildOutput(output)
        }
    }

    override fun copy() = OutputFSM<A, O>(originalStates, false)

    override fun merge(fsMachine: IFsm<A>): OutputFSM<A, O> {
        val states = mutableListOf<IState<A>>()
        states.addAll(this.states)
        if (fsMachine is OutputFSM<A, *>) states.addAll(fsMachine.states)

        return OutputFSM(states)
    }
}

@Suppress("UNCHECKED_CAST")
open class OutputState<A, O>(accepting: Boolean = false,
                             var output: O? = null) : State<A>(accepting) {
    val isOutputting get() = output != null
    val isNotOutputting get() = !isOutputting

    override fun removeAccept() {
        super.removeAccept()
        output = null
    }

    fun replaceOutput(output: O?) {
        accepting = false
        this.output = output
    }

    fun replaceChildOutput(output: O?) {
        findAllChildren().forEach {
            val outputState = it as? OutputState<A, O>
            if (outputState != null && (outputState.isOutputting || outputState.accepting))
                outputState.replaceOutput(output)
        }
    }

    override fun transferToNext(): OutputState<A, O> {
        val newState = OutputState<A, O>(accepting, output)
        newState.transitions.addAll(transitions)
        newState.epsilonTransitions.addAll(epsilonTransitions)

        transitions.clear()
        epsilonTransitions.clear()
        accepting = false
        output = null

        addEpsilonTransition(newState)

        return newState
    }

    override fun replaceWith(other: IState<A>) {
        super.replaceWith(other)

        if (other is OutputState<A, *>)
            output = other.output as? O
    }
}

fun <A : Any, O : Any> Collection<OutputFSM<A, O>>.toMergedFSM() = IFsm.merge(*toTypedArray())