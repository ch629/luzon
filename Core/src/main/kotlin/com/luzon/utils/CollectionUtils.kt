package com.luzon.utils

import com.luzon.fsm.FSM
import com.luzon.fsm.OutputFSM

internal fun <T> Collection<Collection<T>>.merge(): Collection<T> = fold(mutableListOf()) { acc, stateList ->
    acc.addAll(stateList)
    acc
}

internal fun <K : Any, T : Any> Collection<OutputFSM<K, T>>.toMergedFSM() = FSM.merge(*toTypedArray())

internal fun String.toCharList() = toCharArray().toList()

internal fun <T> MutableCollection<T>.replaceWith(other: Collection<T>): Boolean {
    clear()
    return addAll(other)
}