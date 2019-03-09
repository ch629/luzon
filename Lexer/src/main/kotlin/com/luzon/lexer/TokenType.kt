package com.luzon.lexer

import com.luzon.fsm.FSM
import com.luzon.fsm.toMergedFSM
import com.luzon.lexer.Token.*

enum class TokenType {
    KEYWORD, SYMBOL, LITERAL, COMMENT;

    fun toFSM(): FSM<Char, TokenEnum> {
        val machine: FSM<Char, TokenEnum>? = when (this) {
            KEYWORD -> Keyword.values().map { it.toFSM() }.toMergedFSM()
            SYMBOL -> Symbol.values().map { it.toFSM() }.toMergedFSM()
            LITERAL -> Literal.values().map { it.toFSM() }.toMergedFSM()
            COMMENT -> Comment.values().map { it.toFSM() }.toMergedFSM()
        }

        return machine ?: FSM(emptyList())
    }
}