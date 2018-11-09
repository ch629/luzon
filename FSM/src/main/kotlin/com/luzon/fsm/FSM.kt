package com.luzon.fsm

import com.luzon.utils.Predicate
import com.luzon.utils.merge
import com.luzon.utils.replaceWith
import java.util.*

data class Transition<A>(val predicate: Predicate<A>, val state: IState<A>) {
    fun accept(character: A): IState<A>? = if (predicate(character)) state else null
}

open class FSM<A>(protected val states: MutableList<IState<A>> = mutableListOf(),
                  updateEpsilons: Boolean = true) : IFsm<A> {

    protected val originalStates = states.toMutableList()

    constructor(state: IState<A>, updateEpsilons: Boolean = true) : this(mutableListOf(state), updateEpsilons)

    init {
        if (updateEpsilons) updateEpsilons(true)
    }

    override fun merge(fsMachine: IFsm<A>): FSM<A> {
        val states = mutableListOf<IState<A>>()
        states.addAll(this.states)
        if (fsMachine is FSM<A>) states.addAll(fsMachine.states)

        return FSM(states)
    }

    override fun copy() = FSM(originalStates, false)

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

    override fun accept(character: A): Boolean {
        val newStates = states.map { it.accept(character) }.merge()
        states.replaceWith(newStates)

        return updateEpsilons() || newStates.isNotEmpty()
    }

    override val isRunning get() = states.isNotEmpty()
    override val isAccepting get() = states.any { it.accepting }
    override val acceptStates get() = states.filter { it.accepting }

    val stateCount: Int get() = states.size
}

open class State<A>(override var accepting: Boolean = false) : IState<A> {
    protected val transitions = mutableListOf<Transition<A>>()
    protected val epsilonTransitions = mutableListOf<IState<A>>()

    override val leaf: Boolean get() = transitions.isEmpty() && epsilonTransitions.isEmpty()
    override val epsilons get() = epsilonTransitions

    override fun accept(character: A) = transitions.mapNotNull { it.accept(character) }

    fun addTransition(transition: Transition<A>) = transitions.add(transition)

    fun addTransition(predicate: Predicate<A>, state: IState<A>) =
            addTransition(Transition(predicate, state))

    fun addEpsilonTransition(state: IState<A>) = epsilonTransitions.add(state)

    override fun removeAccept() {
        accepting = false
    }

    override fun transferToNext(): State<A> {
        val newState = State<A>(accepting)
        newState.transitions.addAll(transitions)
        newState.epsilonTransitions.addAll(epsilonTransitions)

        transitions.clear()
        epsilonTransitions.clear()
        accepting = false

        addEpsilonTransition(newState)

        return newState
    }

    override fun replaceWith(other: IState<A>) {
        if (other is State<A>) {
            transitions.replaceWith(other.transitions)
            epsilonTransitions.replaceWith(other.epsilonTransitions)
            accepting = other.accepting
        }
    }

    protected fun findAllChildren(): Set<State<A>> {
        val cachedStates = mutableSetOf<State<A>>()
        val stateStack = Stack<State<A>>()
        cachedStates.add(this)
        stateStack.push(this)

        while (stateStack.isNotEmpty()) {
            val currentState = stateStack.pop() as State<A>
            currentState.transitions.forEach { (_, state) ->
                if (state is State<A>)
                    if (cachedStates.add(state)) stateStack.push(state)
            }

            currentState.epsilonTransitions.forEach { state ->
                if (state is State<A>)
                    if (cachedStates.add(state)) stateStack.push(state)
            }
        }

        return cachedStates
    }

    val leaves: List<IState<A>> get() = findAllChildren().filter { it.leaf }
}