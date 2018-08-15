package com.luzon.utils

internal fun rangePredicate(start: Char, end: Char): (Char) -> Boolean = { it in start..end }
internal fun charPredicate(c: Char): (Char) -> Boolean = { it == c }
internal fun orPredicate(first: (Char) -> Boolean, second: (Char) -> Boolean): (Char) -> Boolean = { first(it) || second(it) }

internal fun orPredicate(vararg predicates: (Char) -> Boolean): (Char) -> Boolean {
    var predicate: (Char) -> Boolean = { false }

    predicates.forEach { predicate = predicate or it }

    return predicate
}

internal infix fun ((Char) -> Boolean).or(other: (Char) -> Boolean): (Char) -> Boolean = orPredicate(this, other)
internal infix fun Char.range(other: Char) = rangePredicate(this, other)
internal fun Char.predicate() = charPredicate(this)