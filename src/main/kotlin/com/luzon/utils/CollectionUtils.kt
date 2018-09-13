package com.luzon.utils

import com.luzon.fsm.FSM

internal fun <T> Collection<Collection<T>>.merge(): Collection<T> = fold(mutableListOf()) { acc, stateList ->
    acc.addAll(stateList)
    acc
}

internal fun <K, T> Collection<FSM<K, T>>.toMergedFSM() = FSM.merge(*toTypedArray())

internal fun String.toCharList() = toCharArray().toList()

internal fun <T> MutableCollection<T>.replaceWith(other: Collection<T>): Boolean {
    clear()
    return addAll(other)
}