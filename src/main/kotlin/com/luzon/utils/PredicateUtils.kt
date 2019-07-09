package com.luzon.utils

typealias Predicate<T> = (T) -> Boolean

fun <T : Any> T.equalPredicate(): Predicate<T> = { it == this }
fun rangePredicate(start: Char, end: Char): Predicate<Char> = { it in start..end }

fun <T> orPredicate(vararg predicates: Predicate<T>): Predicate<T> = { it ->
    predicates.any { predicate -> predicate(it) }
}

infix fun <T> Predicate<T>.or(other: Predicate<T>): Predicate<T> = { this(it) || other(it) }
infix fun Char.range(other: Char) = rangePredicate(this, other)
