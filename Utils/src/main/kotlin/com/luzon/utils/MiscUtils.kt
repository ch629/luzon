package com.luzon.utils

import java.util.*

typealias Predicate<T> = (T) -> Boolean

fun <T> Stack<T>.peekOrNull() = if (isEmpty()) null else peek()
fun <T> Stack<T>.popOrNull() = if (isEmpty()) null else pop()

fun StringBuilder.indent(times: Int = 1): StringBuilder = append("    ".repeat(times))

private class IfTest {
    companion object {
        fun <T> T.IfIs(value: T?) = If { it == value }
        fun <T> T.If(pred: Predicate<T>) = PartialIf(this, pred, { it })

        fun <T, K> T.As(conv: (T) -> K) = PrePartialIf(this, conv)

        data class PrePartialIf<K, T>(val value: T, val conv: (T) -> K) {
            fun IfIs(value: T?): PartialIf<K, T> = If { it == value }
            fun If(pred: Predicate<T>): PartialIf<K, T> = PartialIf(value, pred, conv)
        }

        data class PartialIf<K, T>(val value: T, val pred: Predicate<T>, val conv: (T) -> K) {
            fun Else(elseValue: K?) = if (pred(value)) conv(value) else elseValue
            fun ElseNull() = Else(null)
        }

        // Example:
        // fun next() = transitions
        //            .As { it[0].value to it[0].state }
        //            .If { it.isNotEmpty() }
        //            .ElseNull()
    }
}