package com.luzon.recursive_descent

import com.luzon.lexer.Token
import com.luzon.lexer.TokenStream
import com.luzon.utils.Predicate

open class RecursiveDescentStream<T>(tokens: Sequence<T>) {
    private val iterator = tokens.iterator()
    private var lookahead: T? = null
    private var token: T? = null

    init {
        if (token == null && lookahead == null && iterator.hasNext()) // initialize lookahead
            lookahead = iterator.next()

        next()
    }

    fun next(): Boolean {
        token = lookahead
        lookahead = if (iterator.hasNext()) iterator.next() else null

        return token != null
    }

    fun lookaheadMatches(pred: Predicate<T>) = lookahead != null && pred(lookahead!!)

    fun matches(pred: Predicate<T>) = token != null && pred(token!!)

    fun matchConsume(pred: Predicate<T>) = accept(pred) != null

    fun accept(pred: Predicate<T>) = if (matches(pred)) {
        val tmp = token
        next()
        tmp
    } else null

    fun accept(pred: Predicate<T>, block: (T) -> Boolean): Boolean {
        val tkn = accept(pred)
        return tkn != null && block(tkn)
    }

    fun consume() = accept { true }

    fun consume(pred: Predicate<T>) = accept(pred)

    fun isDone() = iterator.hasNext()
}

class TokenRecursiveDescentStream(tokens: TokenStream) : RecursiveDescentStream<Token>(tokens) {
    fun accept(tokenEnum: Token.TokenEnum, block: (Token) -> Boolean) = accept({ it.tokenEnum == tokenEnum }, block)
    fun accept(vararg tokenEnum: Token.TokenEnum) = accept { token -> tokenEnum.any { token.tokenEnum == it } }

    fun matches(tokenEnum: Token.TokenEnum) = matches { it.tokenEnum == tokenEnum }
    fun matchConsume(tokenEnum: Token.TokenEnum) = matchConsume { it.tokenEnum == tokenEnum }

    fun accept(tokenEnum: Token.TokenEnum) = accept { it.tokenEnum == tokenEnum }

    fun lookaheadMatches(vararg tokenEnums: Token.TokenEnum) = lookaheadMatches { token -> tokenEnums.any { token.tokenEnum == it } }
}
