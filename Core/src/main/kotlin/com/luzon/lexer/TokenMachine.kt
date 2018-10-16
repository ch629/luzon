package com.luzon.lexer

import com.luzon.fsm.FSM
import com.luzon.fsm.OutputFSM

@Suppress("UNCHECKED_CAST")
object TokenMachine {
    private val fsm by lazy {
        FSM.merge(
                TokenType.LITERAL.toFSM(),
                TokenType.KEYWORD.toFSM(),
                TokenType.SYMBOL.toFSM(),
                TokenType.COMMENT.toFSM()
        ) as OutputFSM<Char, Token.TokenEnum>
    }

    fun getFSM() = fsm.copy()
}