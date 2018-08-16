package com.luzon.lexer

import com.luzon.Token
import com.luzon.fsm.FSMachine

class Tokenizer(text: String) : Scanner(text) {
    val tokens = mutableListOf<Token>()

    fun findTokens() {
        val tokenizerHelper = FSMTokenizerHelper(this)

        while (!isAtEnd()) { //TODO: Removing comments?
            while (peek() == ' ') advance() //Skip whitespace between any found tokens
            tokens.add(tokenizerHelper.findNextToken())
        }
    }

    fun consume(amount: Int) {
        current += amount
    }

    fun addToken(token: Token) {
        tokens.add(token)
    }
}

class FSMTokenizerHelper(val scanner: Scanner) {
    var machine = FSMachine.fromRegex<Token>("") //TODO: All tokens (Maybe hold the regex inside each Token type, then loop through all tokens and merge to make this)
    var latestToken: Token? = null
    //TODO: Maybe have a save FSM to file, then I can just read that directly in from the initial Regex, rather than scan regex every time?

    fun isRunning(): Boolean = machine.isRunning()

    fun findNextToken(): Token {
        while (isRunning() && !scanner.isAtEnd()) {
            machine.accept(scanner.advance())
            val acceptingStates = machine.acceptingStates()

            if (acceptingStates.isNotEmpty())
                latestToken = acceptingStates.filter { it.output != null }.maxBy { it.output!!.PRIORITY }!!.output!!
        }

        return latestToken!!
    }
}

