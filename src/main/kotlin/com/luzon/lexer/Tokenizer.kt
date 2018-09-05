package com.luzon.lexer

import com.luzon.fsm.StringScanner
import mu.KLogging
import java.nio.file.Files
import java.nio.file.Paths

class Tokenizer(text: String) : StringScanner(text) {
    private val tokenizerHelper = FSMTokenizerHelper(this)

    companion object {
        private val skipChars = arrayOf(
                ' ', '\t', '\n', '\r'
        )

        fun fromFile(file: String) = Tokenizer(Files.readAllLines(Paths.get(file)).joinToString("\n"))
    }

    fun findTokens(): Sequence<Token> {
        tokenizerHelper.reset()

        return generateSequence {
            while (peek() in skipChars && isNotAtEnd()) advance()
            if (isAtEnd()) null
            else tokenizerHelper.findNextToken()
        }
    }

    fun tokensAsString() = findTokens().joinToString(" ")
}

class FSMTokenizerHelper(private val scanner: StringScanner) {
    private val machine = TokenMachine.getFSM()
    //TODO: Maybe have a save FSM to file, then I can just read that directly in from the initial Regex, rather than scan regex every time?

    companion object : KLogging()

    fun findNextToken(): Token {
        val errorBuffer = StringBuffer()
        var token: Token?

        do {
            machine.reset()
            val char = scanner.peek()
            val current = scanner.current
            token = findToken()

            if (token == null)
                errorBuffer.append(char).append(" starting at character ").append(current) //TODO: Better error logging. (Line, character, log all once the tokenizer has finished)
        } while (token == null && scanner.isNotAtEnd()) //Keep trying to find tokens if it finds an invalid character

        if (errorBuffer.isNotEmpty()) logger.warn("Found invalid characters: $errorBuffer")

        return token ?: Token.None.toToken()
    }

    private fun findToken(): Token? {
        var foundToken: Token.TokenEnum? = null
        var foundCurrent = scanner.current + 1 //If there isn't one found, the next character will be checked
        val tokenDataBuffer = StringBuffer()
        val startCurrent = scanner.current

        while (machine.isRunning() && scanner.isNotAtEnd()) {
            val c = scanner.advance()
            machine.accept(c)
            tokenDataBuffer.append(c)
            val acceptingStates = machine.acceptingStates()

            if (acceptingStates.isNotEmpty()) {
                val newToken = acceptingStates.filter { it.output != null }.map { it.output }.first()
//                val newToken = machine.getCurrentOutput().first() //TODO: This?

                if (newToken != null) {
                    foundToken = newToken
                    foundCurrent = scanner.current
                }
            }
        }

        val data = tokenDataBuffer.substring(0, foundCurrent - startCurrent)
        scanner.current = foundCurrent
        return (foundToken ?: Token.None).toToken(data)
    }

    fun reset() {
        machine.reset()
        scanner.current = 0
    }
}
