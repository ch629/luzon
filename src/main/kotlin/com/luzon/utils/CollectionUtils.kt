package com.luzon.utils

internal fun <T> Collection<Collection<T>>.merge(): Collection<T> = fold(mutableListOf()) { acc, stateList ->
    acc.addAll(stateList)
    acc
}