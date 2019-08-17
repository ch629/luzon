package com.luzon.lexer

import com.luzon.fsm.FSM
import com.luzon.lexer.TokenType.COMMENT
import com.luzon.lexer.TokenType.KEYWORD
import com.luzon.lexer.TokenType.LITERAL
import com.luzon.lexer.TokenType.SYMBOL

object TokenMachine {
    private var fsm: FSM<Char, Token.TokenEnum>? = null

    fun getFSM(): FSM<Char, Token.TokenEnum> {
        if (fsm == null) fsm = FSM.merge(
            LITERAL.toFSM(),
            KEYWORD.toFSM(),
            SYMBOL.toFSM(),
            COMMENT.toFSM()
        )

        return fsm!!.copyOriginal()
    }

    fun clearFSM() {
        fsm = null
    }
}
