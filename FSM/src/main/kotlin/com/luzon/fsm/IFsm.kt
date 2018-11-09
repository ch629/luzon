package com.luzon.fsm

import com.luzon.fsm.scanner.RegexScanner

interface IFsm<A> {
    fun accept(character: A): Boolean
    fun merge(fsMachine: IFsm<A>): IFsm<A>
    fun copy(): IFsm<A>
    fun reset()

    val isRunning: Boolean
    val isNotRunning: Boolean get() = !isRunning

    val isAccepting: Boolean
    val isNotAccepting: Boolean get() = !isAccepting

    val acceptStates: List<IState<A>>

    companion object {
        fun <O : Any> fromRegex(str: String) = OutputFSM<Char, O>(RegexScanner<O>(str).toFSM())

        fun <A> merge(vararg machines: IFsm<A>): IFsm<A> = machines.reduce { acc, fsMachine ->
            acc.merge(fsMachine)
        }
    }
}

interface IState<A> {
    fun accept(character: A): List<IState<A>>
    val epsilons: List<IState<A>>

    var accepting: Boolean
    val leaf: Boolean

    fun removeAccept()
    fun transferToNext(): IState<A>
    fun replaceWith(other: IState<A>)
}