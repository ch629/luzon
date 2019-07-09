package com.luzon.utils

fun <T> Collection<Collection<T>>.merge(): Collection<T> = fold(mutableListOf()) { acc, stateList ->
    acc.addAll(stateList)
    acc
}

fun String.toCharList() = toCharArray().toList()

fun <T> MutableCollection<T>.replaceWith(other: Collection<T>): Boolean {
    clear()
    return addAll(other)
}
