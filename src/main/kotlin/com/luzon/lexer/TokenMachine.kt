package com.luzon.lexer

import com.luzon.fsm.FSM
import com.luzon.lexer.TokenType.*

object TokenMachine {
    private val fsm by lazy {
        FSM.merge(
                LITERAL.toFSM(),
                KEYWORD.toFSM(),
                SYMBOL.toFSM(),
                COMMENT.toFSM()
        )
    }

    fun getFSM() = fsm.copyOriginal()
}