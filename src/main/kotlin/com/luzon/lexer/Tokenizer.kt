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

            if (token.tokenEnum is Token.Comment) findNextToken() else token
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

    companion object : KLogging()

    fun findNextToken(): Token {
        machine.reset()
        val char = scanner.peek()
        val current = scanner.current
        val token = findToken()

        // TODO: Throw error or return Either
        if (token.tokenEnum == Token.None)
            logger.error("Invalid token: $char starting at character $current")

        return token
    }

    private fun findToken(): Token {
        var foundToken: Token.TokenEnum? = null
        var foundCurrent = scanner.current + 1
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
        val data = if (diff <= tokenDataBuffer.length) tokenDataBuffer.substring(0, diff) else ""
        scanner.current = foundCurrent
        return (foundToken ?: Token.None).toToken(data)
    }

    fun reset() {
        machine.reset()
        scanner.current = 0
    }
}
