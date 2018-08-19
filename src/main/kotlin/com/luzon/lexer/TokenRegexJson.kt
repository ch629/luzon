package com.luzon.lexer

import com.luzon.fsm.FSMachine
import com.luzon.kodein
import com.squareup.moshi.Moshi
import org.kodein.di.generic.instance
import java.nio.file.Files
import java.nio.file.Paths

data class TokenRegexJson(
        val literals: Map<String, String>,
        val keywords: Map<String, String>,
        val symbols: Map<String, String>,
        val comments: Map<String, String>
) {
    companion object {
        fun fromJson(): TokenRegexJson {
            val fileText = Files.readAllLines(Paths.get(TokenRegexJson::class.java.getResource("tokens.json").toURI())).joinToString(" ")
            val moshi: Moshi by kodein.instance()
            val adapter = moshi.adapter<TokenRegexJson>(TokenRegexJson::class.java)

            return adapter.fromJson(fileText)!!
        }
    }

    fun toFSM() = FSMachine.merge(
            literals.toFSM(TokenType.LITERAL),
            keywords.toFSM(TokenType.KEYWORD, true),
            symbols.toFSM(TokenType.SYMBOL, true),
            comments.toFSM(TokenType.COMMENT))

    private fun Map<String, String>.toFSM(name: TokenType, plainText: Boolean = false): FSMachine<TokenHolder> {
        val map = map { (tokenName, regex) ->
            val token = getToken(tokenName, name)
            val usedRegex = if (plainText) replaceMetacharacters(regex) else regex

            FSMachine.fromRegex<TokenHolder>(usedRegex).setOutput(token)
        }

        return FSMachine.merge(*map.toTypedArray())
    }

    private fun replaceMetacharacters(regex: String): String {
        var newRegex = regex

        "\\*+?[]().".forEach {
            newRegex = newRegex.replace("$it", "\\$it")
        }

        return newRegex
    }

    private fun getToken(name: String, type: TokenType): TokenHolder {
        val token = findToken(name, type)

        return TokenHolder(when (token) {
            is Literal -> LiteralContainer(token)
            is Keyword -> KeywordContainer(token)
            is Symbol -> SymbolContainer(token)
            else -> None
        })
    }
}