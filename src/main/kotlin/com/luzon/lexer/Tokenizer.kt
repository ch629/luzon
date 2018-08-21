package com.luzon.lexer

import com.luzon.kodein
import org.kodein.di.generic.instance
import java.nio.file.Files
import java.nio.file.Paths

fun main(args: Array<String>) {
    val tokenizer = Tokenizer.fromFile("C:\\Programming\\luzon\\code_examples\\One.lz")

    tokenizer.findTokens()
    tokenizer.print()
}

class Tokenizer(text: String) : Scanner(text) {
    private val tokens = mutableListOf<TokenHolder>()

    companion object {
        private val skipChars = arrayOf(
                ' ', '\t', '\n', '\r'
        )

        fun fromFile(file: String) = Tokenizer(Files.readAllLines(Paths.get(file)).joinToString("\n"))
    }

    init {
        println("Tokenizer Read:\n$text")
    }

    fun findTokens() {
        val tokenizerHelper = FSMTokenizerHelper(this)

        while (!isAtEnd()) {
            while (peek() in skipChars) advance() //Skip whitespace between any found tokens
            tokens.add(tokenizerHelper.findNextToken())
        }
    }

    fun tokensAsString() = tokens.joinToString(" ") { it.toString() }

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

