package com.luzon.fsm

import com.luzon.lexer.*

//TODO: TokenEnum should be an ASTNode Type really
class TokenScanner(tokens: List<MetaTokenContainer>) : MetaScanner<MetaTokenContainer, TokenEnum>(tokens, MetaTokenContainer(token = None.toToken())) {
    companion object {
        fun fromList(values: List<Any>) = fromList(*values.toTypedArray())

        fun fromList(vararg values: Any) = TokenScanner(values.map {
            when (it) {
                is Token -> MetaTokenContainer(token = it)
                is TokenEnum -> MetaTokenContainer(token = it.toToken())
                is MetaCharacter -> MetaTokenContainer(metaCharacter = it)
                else -> MetaTokenContainer()
            }
        })
    }

    override val orPredicate: (MetaTokenContainer) -> Boolean
        get() = { it.metaCharacter == MetaCharacter.OR }
    override val kleeneStarPredicate: (MetaTokenContainer) -> Boolean
        get() = { it.metaCharacter == MetaCharacter.KLEENE_STAR }
    override val kleenePlusPredicate: (MetaTokenContainer) -> Boolean
        get() = { it.metaCharacter == MetaCharacter.KLEENE_PLUS }
    override val optionalPredicate: (MetaTokenContainer) -> Boolean
        get() = { it.metaCharacter == MetaCharacter.OPTIONAL }
    override val startGroupPredicate: (MetaTokenContainer) -> Boolean
        get() = { it.metaCharacter == MetaCharacter.START_GROUP }
    override val endGroupPredicate: (MetaTokenContainer) -> Boolean
        get() = { it.metaCharacter == MetaCharacter.END_GROUP }
    override val escapePredicate: (MetaTokenContainer) -> Boolean
        get() = { it.metaCharacter == MetaCharacter.ESCAPE }

    override fun createScanner(text: List<MetaTokenContainer>) = TokenScanner(text)
}

data class MetaTokenContainer(val metaCharacter: MetaCharacter = MetaCharacter.NONE, val token: Token? = null) {
    override fun equals(other: Any?): Boolean {
        if (other !is MetaTokenContainer) return false
        if ((other.token == null) xor (token == null)) return false
        if (token == null) return true
        return other.metaCharacter == metaCharacter || other.token!!.tokenEnum == this.token.tokenEnum
    }

    override fun hashCode(): Int {
        var result = metaCharacter.hashCode()
        result = 31 * result + (token?.hashCode() ?: 0)
        return result
    }
}

enum class MetaCharacter {
    OR,
    KLEENE_STAR,
    KLEENE_PLUS,
    OPTIONAL,
    START_GROUP,
    END_GROUP,
    ESCAPE,
    NONE
}

fun main(args: Array<String>) {
    val scanner = TokenScanner.fromList(
            Keyword.FOR,
            Symbol.L_PAREN,
            Keyword.VAR,
            Literal.IDENTIFIER,
            Keyword.IN,
            Literal.INT,
            Symbol.RANGE,
            Literal.INT,
            Symbol.R_PAREN
    ) //for(var i = 0)

    val machine = FSMachine(scanner.toFSM())
    val code = "for(var i in 0..5)"
    val tokenizer = Tokenizer(code)

    println(tokenizer.tokensAsString())
    val tokens = tokenizer.findTokens()

    tokens.forEach {
        machine.accept(MetaTokenContainer(token = it))
    }

    println(machine.isAccepting())
}