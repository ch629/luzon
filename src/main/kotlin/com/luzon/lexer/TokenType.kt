package com.luzon.lexer

import com.luzon.fsm.FiniteStateMachine
import com.luzon.lexer.Token.Comment
import com.luzon.lexer.Token.Keyword
import com.luzon.lexer.Token.Literal
import com.luzon.lexer.Token.Symbol

enum class TokenType {
    KEYWORD, SYMBOL, LITERAL, COMMENT;

    fun toFSM() = when (this) {
        KEYWORD -> Keyword.values().map { it.toFSM() }.toMergedFSM()
        SYMBOL -> Symbol.values().map { it.toFSM() }.toMergedFSM()
        LITERAL -> Literal.values().map { it.toFSM() }.toMergedFSM()
        COMMENT -> Comment.values().map { it.toFSM() }.toMergedFSM()
    }
}

private fun <A : Any, O : Any> Collection<FiniteStateMachine<A, O>>.toMergedFSM() = FiniteStateMachine.merge(*toTypedArray())
