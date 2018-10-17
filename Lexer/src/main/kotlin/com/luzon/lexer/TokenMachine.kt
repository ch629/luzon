package com.luzon.lexer

import com.luzon.fsm.FSM
import com.luzon.fsm.OutputFSM
import com.luzon.lexer.TokenType.*

@Suppress("UNCHECKED_CAST")
object TokenMachine {
    private val fsm by lazy {
        FSM.merge(
                LITERAL.toFSM(),
                KEYWORD.toFSM(),
                SYMBOL.toFSM(),
                COMMENT.toFSM()
        ) as OutputFSM<Char, Token.TokenEnum>
    }

    fun getFSM() = fsm.copy()
}