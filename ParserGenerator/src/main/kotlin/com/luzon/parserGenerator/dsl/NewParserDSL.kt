package com.luzon.parserGenerator.dsl

import com.luzon.lexer.Token

// Call this a wrapper?
sealed class ParserAlphabetWrapper { // TODO: Make this private and only used internally
    fun name() = when (this) {
        is AlphabetToken -> tokenEnum.id()!!.toUpperCase()
        is AlphabetDSL -> dsl.name
    }

    fun stringName() = when (this) {
        is AlphabetToken -> tokenEnum.id()!!.toUpperCase()
        is AlphabetDSL -> "<${dsl.name}>"
    }

    fun paramTypeName() =
            (if (this is AlphabetToken) tokenEnum.id()!! else name()).capitalize()

    fun isLiteral() = this is AlphabetToken && tokenEnum is Token.Literal
}

private data class AlphabetToken(val tokenEnum: Token.TokenEnum) : ParserAlphabetWrapper()
private data class AlphabetDSL(val dsl: NewParserDSL) : ParserAlphabetWrapper()

class NewParserDSL(val name: String) {
    private val definitions = hashMapOf<String, MutableList<List<ParserAlphabetWrapper>>>()

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
        addDefinition(dsl.name, AlphabetDSL(dsl))
    }

    private fun addDefinition(name: String, vararg tokenEnums: Token.TokenEnum) {
        addDefinition(name, tokenEnums.map { AlphabetToken(it) })
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
        tokens.add(AlphabetToken(this))
    }

    operator fun NewParserDSL.unaryPlus() {
        tokens.add(AlphabetDSL(this))
    }
}

class DSLSection(val name: String, val optional: Boolean = false) {
    private val sectionDefinitions = mutableListOf<DSLSection>()
    private val baseDefinition = mutableListOf<ParserAlphabetWrapper>()


}

// Empty name = default definition (no name)
class Definition(val name: String = "") {
    // Order of types (including optionals and grouped)
}