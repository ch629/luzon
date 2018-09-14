package com.luzon.utils

typealias Predicate<T> = (T) -> Boolean

internal fun <T : Any> T.equalPredicate(): Predicate<T> = { it == this }
internal fun rangePredicate(start: Char, end: Char): Predicate<Char> = { it in start..end }

internal fun <T> orPredicate(vararg predicates: Predicate<T>) = predicates.reduce { acc, predicate ->
    acc or predicate
}

internal infix fun <T> Predicate<T>.or(other: Predicate<T>): Predicate<T> = { this(it) || other(it) }
internal infix fun Char.range(other: Char) = rangePredicate(this, other)