package com.luzon.parser

import com.luzon.lexer.Token

// Call this a wrapper?
sealed class ParserAlphabetWrapper { // TODO: Make this private and only used internally
    data class TokenEnum(val tokenEnum: Token.TokenEnum) : ParserAlphabetWrapper()
    data class ParserDSL(val dsl: NewParserDSL) : ParserAlphabetWrapper()

    fun name() = when (this) {
        is TokenEnum -> tokenEnum.id()!!.toUpperCase()
        is ParserDSL -> dsl.name
    }

    fun stringName() = when (this) {
        is TokenEnum -> tokenEnum.id()!!.toUpperCase()
        is ParserDSL -> "<${dsl.name}>"
    }

    fun paramTypeName() =
            (if (this is ParserAlphabetWrapper.TokenEnum) tokenEnum.id()!! else name()).capitalize()

    fun isLiteral() = this is ParserAlphabetWrapper.TokenEnum && tokenEnum is Token.Literal
}

class NewParserDSL(val name: String) {
    val definitions = hashMapOf<String, MutableList<List<ParserAlphabetWrapper>>>()

    private fun defDsl(init: NewParserDefDSL.() -> Unit) = NewParserDefDSL(this).apply(init)

    private fun getDefinition(name: String): MutableList<List<ParserAlphabetWrapper>> {
        if (!definitions.containsKey(name)) definitions[name] = mutableListOf()
        return definitions[name]!!
    }

    private fun addDefinition(name: String, characters: List<ParserAlphabetWrapper>) {
        getDefinition(name).add(characters)
    }

    private fun addDefinition(name: String, character: ParserAlphabetWrapper) {
        addDefinition(name, listOf(character))
    }

    private fun addDefinition(dsl: NewParserDSL) {
        addDefinition(dsl.name, ParserAlphabetWrapper.ParserDSL(dsl))
    }

    private fun addDefinition(name: String, vararg tokenEnums: Token.TokenEnum) {
        addDefinition(name, tokenEnums.map { ParserAlphabetWrapper.TokenEnum(it) })
    }

    fun def(dsl: NewParserDSL) {
        addDefinition(dsl)
    }

    // TODO: Some won't have names, so need a way to deal with that. I could have a defaultDefinition which just
    // contains lists of characters, or potentially a name of self or the parserDsl name could be used here
    fun def(tokenEnum: Token.TokenEnum) {
        TODO("Name?")
    }

    fun defOr(init: NewParserDefDSL.() -> Unit) {
        val defDsl = defDsl(init)
        addDefinition(name, defDsl.tokens)
        TODO()
    }
}

class NewParserDefDSL(private val forParser: NewParserDSL) {
    val tokens = mutableListOf<ParserAlphabetWrapper>()

    operator fun Token.TokenEnum.unaryPlus() {
        tokens.add(ParserAlphabetWrapper.TokenEnum(this))
    }

    operator fun NewParserDSL.unaryPlus() {
        tokens.add(ParserAlphabetWrapper.ParserDSL(this))
    }
}