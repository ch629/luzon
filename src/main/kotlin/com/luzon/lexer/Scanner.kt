package com.luzon.lexer

open class Scanner(private val text: String) {
    var current = 0

    fun advance(): Char {
        val char = peek()
        current++
        return char
    }

    fun peek() = if (!isAtEnd()) text[current] else '\n'

    fun isAtEnd() = current >= text.length
}