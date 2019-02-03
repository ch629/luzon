package com.luzon.rd

import com.luzon.lexer.Token
import com.luzon.lexer.TokenStream
import com.luzon.utils.Predicate

// TODO: Come up with a better name
open class RDStream<T>(tokens: Sequence<T>) { // TODO: All return null using this?
    private val iterator = tokens.iterator()
    private var token: T? = null

    init {
        next()
    }

    fun next(): Boolean {
        token = if (iterator.hasNext()) iterator.next() else null
        return token != null
    }

    fun matches(pred: Predicate<T>) = token != null && pred(token!!)
    fun matches(pred: Predicate<T>, block: (T) -> Boolean) = if (matches(pred) && token != null) block(token!!) else false

    fun matchConsume(pred: Predicate<T>) = accept(pred) != null

    fun accept(pred: Predicate<T>) = if (matches(pred)) {
        val tmp = token
        next()
        tmp
    } else null

    fun accept(pred: Predicate<T>, block: (T) -> Boolean): Boolean {
        val tkn = accept(pred)
        return if (tkn != null) block(tkn) else false
    }

    fun consume() = accept { true }

    fun consume(pred: Predicate<T>) = accept(pred)
}

class TokenRDStream(tokens: TokenStream) : RDStream<Token>(tokens) {
    fun accept(tokenEnum: Token.TokenEnum, block: (Token) -> Boolean) = accept({ it.tokenEnum == tokenEnum }, block)
    fun matches(tokenEnum: Token.TokenEnum) = matches { it.tokenEnum == tokenEnum }
    fun matches(tokenEnum: Token.TokenEnum, block: (Token) -> Boolean) = matches({ it.tokenEnum == tokenEnum }, block)
    fun matchConsume(tokenEnum: Token.TokenEnum) = matchConsume { it.tokenEnum == tokenEnum }
}

class ExpressionRDStream(tokens: ExpressionStream) : RDStream<ExpressionToken>(tokens) {
    fun matchConsume(tokenEnum: Token.TokenEnum) = matchConsume { it.tokenType == tokenEnum }
}