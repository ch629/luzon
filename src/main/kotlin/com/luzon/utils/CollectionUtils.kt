package com.luzon.utils

import com.luzon.fsm.FSMachine

internal fun <T> Collection<Collection<T>>.merge(): Collection<T> = fold(mutableListOf()) { acc, stateList ->
    acc.addAll(stateList)
    acc
}

internal fun <K, T> Collection<FSMachine<K, T>>.toMergedFSM() = FSMachine.merge(*toTypedArray())

internal fun String.toCharList() = toCharArray().toList()