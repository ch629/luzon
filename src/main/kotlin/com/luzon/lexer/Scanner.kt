package com.luzon.lexer

abstract class Scanner(val text: String) {
    var current = 0

    fun advance(): Char {
        val char = peek()
        current++
        return char
    }

    fun peek() = if (!isAtEnd()) text[current] else '\n'

    fun isAtEnd() = current >= text.length
}