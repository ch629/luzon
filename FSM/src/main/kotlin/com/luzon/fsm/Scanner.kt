package com.luzon.fsm

import com.luzon.utils.toCharList

open class Scanner<T>(private val text: List<T>, private val endValue: T) {
    var current = 0

    fun advance(): T {
        val char = peek()
        current++
        return char
    }

    fun peek() = if (isAtEnd()) endValue else text[current]

    fun isAtEnd() = current >= text.size
    fun isNotAtEnd() = !isAtEnd()

    /**
     * Copies the scanner at the specific location
     */
    fun copy(): Scanner<T> = Scanner(text, endValue).apply { current = this@Scanner.current }
}

open class StringScanner(text: String) : Scanner<Char>(text.toCharList(), '\n')