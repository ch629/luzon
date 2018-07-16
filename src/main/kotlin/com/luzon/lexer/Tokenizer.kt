package com.luzon.lexer

import com.luzon.Token

class Tokenizer {
    var current = 0

    fun advance() {
        current++
    }

    fun peek(): Char {
        TODO()
    }

    fun isAtEnd(): Boolean {
        TODO()
    }

    fun consume(amount: Int) {
        current += amount
    }

    fun addToken(token: Token) {
        TODO()
    }
}

class FSMTokenizerHelper {
    //TODO
}

class ExpressionTokenizer {
    private val float = Regex("-?[0-9]+\\.?[0-9]*f")
    private val double = Regex("-?[0-9]+\\.?[0-9]*")
    private val int = Regex("-?[0-9]+")
}