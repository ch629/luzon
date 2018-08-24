package com.luzon.utils

internal fun <T> predicate(value: T): (T) -> Boolean = { it == value }
internal fun rangePredicate(start: Char, end: Char): (Char) -> Boolean = { it in start..end }

internal fun <T> orPredicate(vararg predicates: (T) -> Boolean): (T) -> Boolean {
    var predicate: (T) -> Boolean = { false }

    predicates.forEach { predicate = predicate or it }

    return predicate
}

internal infix fun <T> ((T) -> Boolean).or(other: (T) -> Boolean): (T) -> Boolean = { this(it) || other(it) }
internal infix fun Char.range(other: Char) = rangePredicate(this, other)
internal fun Char.predicate() = predicate(this)