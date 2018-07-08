package com.luzon

import java.util.stream.Stream

class ExpressionTokenizer {
    private val float = Regex("-?[0-9]+\\.?[0-9]*f")
    private val double = Regex("-?[0-9]+\\.?[0-9]*")
    private val int = Regex("-?[0-9]+")

    fun toTokenStream(str: String): Stream<Token> {
        TODO("Maybe not a stream, probably best to use an FSM")
    }
}