package com.luzon.lexer

import com.luzon.kodein
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

    init {
        println("Tokenizer Read:\n$text")
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
    private var latestToken: TokenHolder = TokenHolder(None)
    private var latestCurrent: Int = 0
    private val machine = machineTemplate.copy()
    //TODO: Maybe have a save FSM to file, then I can just read that directly in from the initial Regex, rather than scan regex every time?

    companion object {
        private val regexJson: TokenRegexJson by kodein.instance()
        private val machineTemplate = regexJson.toFSM()
    }

    fun findNextToken(): TokenHolder {
        machine.reset()
        while (machine.isRunning() && !scanner.isAtEnd()) {
            machine.accept(scanner.advance())
            val acceptingStates = machine.acceptingStates()

            if (acceptingStates.isNotEmpty()) {
                val tmp = acceptingStates.filter { it.output != null }.map { it.output }.first()

                if (tmp != null) {
                    latestToken = tmp
                    latestCurrent = scanner.current
                }
            }
        }

        scanner.current = latestCurrent //Back to where the last found token was
        return latestToken
    }
}

