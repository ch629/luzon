package com.luzon.lexer

import com.luzon.fsm.FiniteStateMachine
import com.luzon.lexer.TokenType.COMMENT
import com.luzon.lexer.TokenType.KEYWORD
import com.luzon.lexer.TokenType.LITERAL
import com.luzon.lexer.TokenType.SYMBOL

object TokenMachine {
    private var finiteStateMachine: FiniteStateMachine<Char, Token.TokenEnum>? = null

    fun getFSM(): FiniteStateMachine<Char, Token.TokenEnum> {
        if (finiteStateMachine == null) finiteStateMachine = FiniteStateMachine.merge(
            LITERAL.toFSM(),
            KEYWORD.toFSM(),
            SYMBOL.toFSM(),
            COMMENT.toFSM()
        )

        return finiteStateMachine!!.copyOriginal()
    }

    fun clearFSM() {
        finiteStateMachine = null
    }
}
