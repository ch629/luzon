package com.luzon.lexer

import com.luzon.kodein
import mu.KLogging
import org.kodein.di.generic.instance
import java.nio.file.Files
import java.nio.file.Paths

class Tokenizer(text: String) : Scanner(text) {
    private val tokenizerHelper = FSMTokenizerHelper(this)

    companion object {
        private val skipChars = arrayOf(
                ' ', '\t', '\n', '\r'
        )

        fun fromFile(file: String) = Tokenizer(Files.readAllLines(Paths.get(file)).joinToString("\n"))
    }

    fun findTokens() = generateSequence {
        while (peek() in skipChars && !isAtEnd()) advance()
        if (isAtEnd()) null
        else tokenizerHelper.findNextToken()
    }

    fun tokensAsString() = findTokens().joinToString(" ")

    fun print() {
        println(tokensAsString())
    }
}

class FSMTokenizerHelper(private val scanner: Scanner) {
    private val machine = machineTemplate.copy()
    //TODO: Maybe have a save FSM to file, then I can just read that directly in from the initial Regex, rather than scan regex every time?

    companion object : KLogging() {
        private val regexJson: TokenRegexJson by kodein.instance()
        private val machineTemplate = regexJson.toFSM()
    }

    fun findNextToken(): TokenHolder {
        val stringBuffer = StringBuffer()
        var token: TokenHolder?

        do {
            machine.reset()
            val char = scanner.peek()
            val current = scanner.current
            token = findToken()

            if (token == null)
                stringBuffer.append(char).append(" starting at character ").append(current) //TODO: Better error logging. (Line, character, log all once the tokenizer has finished)

        } while (token == null && !scanner.isAtEnd()) //Keep trying to find tokens if it finds an invalid character

        if (stringBuffer.isNotEmpty()) logger.warn("Found invalid characters: $stringBuffer")

        return token ?: TokenHolder(None)
    }

    private fun findToken(): TokenHolder? {
        var foundToken: TokenHolder? = null
        var foundCurrent = scanner.current + 1 //If there isn't one found, the next character will be checked

        while (machine.isRunning() && !scanner.isAtEnd()) {
            machine.accept(scanner.advance())
            val acceptingStates = machine.acceptingStates()

            if (acceptingStates.isNotEmpty()) {
                val newToken = acceptingStates.filter { it.output != null }.map { it.output }.first()

                if (newToken != null) {
                    foundToken = newToken
                    foundCurrent = scanner.current
                }
            }
        }

        scanner.current = foundCurrent
        return foundToken
    }
}

