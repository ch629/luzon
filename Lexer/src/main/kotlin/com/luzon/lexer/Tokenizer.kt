package com.luzon.lexer

import com.luzon.fsm.scanner.StringScanner
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

    private fun findNextToken(): Token? {
        while (peek() in skipChars && isNotAtEnd()) advance()
        return if (isAtEnd()) null
        else {
            val token = tokenizerHelper.findNextToken()

            return if (token.tokenEnum is Token.Comment) findNextToken() else token
        }
    }

    fun findTokens(): TokenStream {
        tokenizerHelper.reset()

        return generateSequence { findNextToken() }
    }

    fun tokensAsString() = findTokens().joinToString(" ")
}

class FSMTokenizerHelper(private val scanner: StringScanner) {
    private val machine = TokenMachine.getFSM()
    //TODO: Maybe have a save IFsm to file, then I can just read that directly in from the initial Regex, rather than scan regex every time?

    companion object : KLogging()

    //TODO: Better error logging. (Line, character, log all once the tokenizer has finished) Using Either?
    fun findNextToken(): Token {
        val errorBuffer = StringBuffer()
        var token: Token?

        do {
            machine.reset()
            val char = scanner.peek()
            val current = scanner.current
            token = findToken()

            if (token == null)
                errorBuffer.append(char).append(" starting at character ").append(current)
        } while (token == null && scanner.isNotAtEnd()) //Keep trying to find tokens if it finds an invalid character

        if (errorBuffer.isNotEmpty()) logger.warn("Found invalid characters: $errorBuffer")

        return token ?: Token.None.toToken()
    }

    private fun findToken(): Token? {
        var foundToken: Token.TokenEnum? = null
        var foundCurrent = scanner.current + 1 //If there isn't one found, the next character will be checked
        val tokenDataBuffer = StringBuffer()
        val startCurrent = scanner.current

        while (machine.running && scanner.isNotAtEnd()) {
            val c = scanner.advance()
            machine.accept(c)
            tokenDataBuffer.append(c)

            val newToken = machine.acceptValue
            if (newToken != null) {
                foundToken = newToken
                foundCurrent = scanner.current
            }
        }

        val diff = foundCurrent - startCurrent
        val data = if (diff < tokenDataBuffer.length) tokenDataBuffer.substring(0, diff) else ""
        scanner.current = foundCurrent
        return (foundToken ?: Token.None).toToken(data)
    }

    fun reset() {
        machine.reset()
        scanner.current = 0
    }
}
