package com.luzon.lexer

import com.luzon.fsm.FSM

object TokenMachine {
    private val fsm = FSM.merge(
            TokenType.LITERAL.toFSM(),
            TokenType.KEYWORD.toFSM(),
            TokenType.SYMBOL.toFSM(),
            TokenType.COMMENT.toFSM()
    )

    fun getFSM() = fsm.copy() //So multiple machines can run at the same time
}