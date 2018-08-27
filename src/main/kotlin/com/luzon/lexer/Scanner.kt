package com.luzon.lexer

import com.luzon.utils.toCharList

open class Scanner<T>(private val text: List<T>, private val endValue: T) {
    var current = 0

    fun advance(): T {
        val char = peek()
        current++
        return char
    }

    fun peek() = if (!isAtEnd()) text[current] else endValue

    fun isAtEnd() = current >= text.size
}

open class StringScanner(text: String) : Scanner<Char>(text.toCharList(), '\n')