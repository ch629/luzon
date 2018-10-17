package com.luzon.lexer

import com.luzon.fsm.OutputFSM
import com.luzon.fsm.toMergedFSM
import com.luzon.lexer.Token.*

@Suppress("UNCHECKED_CAST")
enum class TokenType {
    KEYWORD, SYMBOL, LITERAL, COMMENT;

    fun toFSM(): OutputFSM<Char, TokenEnum> {
        val machine: OutputFSM<Char, TokenEnum>? = when (this) {
            KEYWORD -> Keyword.values().map { it.toFSM() }.toMergedFSM() as OutputFSM<Char, TokenEnum>
            SYMBOL -> Symbol.values().map { it.toFSM() }.toMergedFSM() as OutputFSM<Char, TokenEnum>
            LITERAL -> Literal.values().map { it.toFSM() }.toMergedFSM() as OutputFSM<Char, TokenEnum>
            COMMENT -> Comment.values().map { it.toFSM() }.toMergedFSM() as OutputFSM<Char, TokenEnum>
        }

        return machine ?: OutputFSM()
    }
}