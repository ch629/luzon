package com.luzon.lexer

import com.luzon.exceptions.InvalidTokenException
import com.luzon.fsm.scanner.StringScanner
import mu.KLogging
import java.nio.file.Files
import java.nio.file.Paths

class Tokenizer(text: String) {
    private val scanner = StringScanner(text)
    private val tokenizerHelper = FSMTokenizerHelper(scanner)

    companion object {
        private val skipChars = arrayOf(
            ' ', '\t', '\n', '\r'
        )

        fun fromFile(file: String) = Tokenizer(Files.readAllLines(Paths.get(file)).joinToString("\n"))
    }

    @Throws(InvalidTokenException::class)
    private fun findNextToken(): Token? {
        while (scanner.peek() in skipChars && scanner.isNotAtEnd()) scanner.advance()
        return if (scanner.isAtEnd()) null
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

    @Throws(InvalidTokenException::class)
    fun findNextToken(): Token {
        machine.reset()

        return findToken()
    }

    @Throws(InvalidTokenException::class)
    private fun findToken(): Token {
        var foundToken: Token.TokenEnum? = null
        var foundCurrent = scanner.current + 1
        val tokenDataBuffer = StringBuilder()
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

        if (foundToken == null)
            throw InvalidTokenException(tokenDataBuffer.toString(), startCurrent)

        return foundToken.toToken(data)
    }

    fun reset() {
        machine.reset()
        scanner.current = 0
    }
}
