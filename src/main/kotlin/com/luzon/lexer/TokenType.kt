package com.luzon.lexer

import com.luzon.fsm.FSM
import com.luzon.lexer.Token.*

enum class TokenType {
    KEYWORD, SYMBOL, LITERAL, COMMENT;

    fun toFSM() = when (this) {
        KEYWORD -> Keyword.values().map { it.toFSM() }.toMergedFSM()
        SYMBOL -> Symbol.values().map { it.toFSM() }.toMergedFSM()
        LITERAL -> Literal.values().map { it.toFSM() }.toMergedFSM()
        COMMENT -> Comment.values().map { it.toFSM() }.toMergedFSM()
    }
}

private fun <A : Any, O : Any> Collection<FSM<A, O>>.toMergedFSM() = FSM.merge(*toTypedArray())