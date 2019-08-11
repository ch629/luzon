package com.luzon.lexer

import com.luzon.fsm.FSM
import com.luzon.lexer.TokenType.COMMENT
import com.luzon.lexer.TokenType.KEYWORD
import com.luzon.lexer.TokenType.LITERAL
import com.luzon.lexer.TokenType.SYMBOL

// TODO: A way to clean this, so if an application no longer requires code to be parsed,
//  then this can be removed to reduce memory usage.
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
