package com.luzon.fsm.scanner

import com.luzon.utils.toCharList

open class Scanner<A>(private val text: List<A>, private val endValue: A) {
    var current = 0

    fun advance(): A {
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
    fun copy(): Scanner<A> = Scanner(text, endValue).apply { current = this@Scanner.current }
}

open class StringScanner(text: String) : Scanner<Char>(text.toCharList(), '\n')