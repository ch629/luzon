package com.luzon.lexer

import com.luzon.Token
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

    fun toFSM() = FSMachine.merge(literals.toFSM(), keywords.toFSM(true), symbols.toFSM(true), comments.toFSM())

    private fun Map<String, String>.toFSM(plainText: Boolean = false): FSMachine<Token> {
        val map = map { (token, regex) ->
            if (plainText)
                replaceMetacharacters(regex)
            FSMachine.fromRegex<Token>(regex).setOutput(getToken(token))
        }

        return FSMachine.merge(*map.toTypedArray())
    }

    private fun replaceMetacharacters(regex: String) {
        "\\*+?[]()".forEach {
            regex.replace("$it", "\\$it")
        }
    }

    //TODO: Need to differentiate between the literal int and the keyword int (May need to append the name to literals:<name> would be one solution)
    private fun getToken(name: String): Token = TODO()
}