package com.luzon.parser

import com.luzon.lexer.Token.Literal
import com.luzon.lexer.Token.Symbol.*
import com.luzon.lexer.Token.TokenEnum

sealed class FSMAlphabet {
    data class AlphabetTokenEnum(val tokenEnum: TokenEnum) : FSMAlphabet()
    data class AlphabetParserDSL(val dsl: ParserDSL) : FSMAlphabet()

    override fun toString(): String {
        return when (this) {
            is AlphabetTokenEnum -> tokenEnum.id()!!.toUpperCase()
            is AlphabetParserDSL -> dsl.name
        }
    }
}

class ParserDSL(val name: String) {
    private val definitions = hashMapOf<String, State>()

    private fun defDsl(init: ParserDefDSL.() -> Unit): ParserDefDSL {
        val def = ParserDefDSL(this)
        def.init()
        return def
    }

    // TODO: How to deal with two definitions with the same name?
    // Potentially turn the States into real state machine states with multiple transitions,
    // so I can just use an or between the two definitions?
    fun def(name: String, init: ParserDefDSL.() -> Unit) {
        val def = defDsl(init)
        definitions[name + this.name] = State.fromAlphabet(*def.tokens.toTypedArray())
    }

    fun def(dsl: ParserDSL) {
        val newState = State.fromDSL(dsl)
        if (definitions.containsKey(dsl.name)) {
            // TODO: Duplicate definitions
        } else definitions[dsl.name] = newState
    }

    fun defOr(init: ParserDefDSL.() -> Unit) {
        val def = defDsl(init)

        addDefinitionOr(def.tokens)
    }

    private fun addDefinitionOr(characters: List<FSMAlphabet>) {
        characters.forEach { character ->
            when (character) {
                is FSMAlphabet.AlphabetParserDSL ->
                    definitions[character.dsl.name.decapitalize()] = State.fromDSL(character.dsl)
                is FSMAlphabet.AlphabetTokenEnum ->
                    definitions[name + character.tokenEnum.id()!!.capitalize()] = State.fromTokens(character.tokenEnum)
            }
        }
    }

    override fun toString(): String = StringBuffer().apply {
        val indent = " ".repeat(name.length + 3)
        append(name)
        append(" ::= ")

        val values = definitions.map { (name, tokenStates) ->
            name to tokenStates.traverse().joinToString(" ") { (character, _) ->
                when (character) {
                    is FSMAlphabet.AlphabetParserDSL -> character.dsl.name.decapitalize()
                    is FSMAlphabet.AlphabetTokenEnum -> character.tokenEnum.id()!!.toUpperCase()
                }
            }
        }

        val longest = values.maxBy { it.second.length }?.second?.length ?: 0

        values.forEachIndexed { index, (name, tokenString) ->
            if (index > 0) {
                append(indent)
                append("| ")
            }

            append(tokenString)
            val spaces = Math.max(0, longest - tokenString.length)
            append(" ".repeat(spaces))
            append(" #")
            append(name)
            appendln()
        }
    }.toString()
}

class ParserDefDSL(private val forParser: ParserDSL) {
    private var rootState = State()
    private var pointer = rootState

    val tokens get() = rootState.traverse().map { it.first }

    operator fun TokenEnum.unaryPlus() {
        val newState = State()
        pointer.addTransition(this, newState)
        pointer = newState
    }

    operator fun ParserDSL.unaryPlus() {
        val newState = State()
        pointer.addTransition(this, newState)
        pointer = newState
    }

    val self: ParserDSL get() = forParser
}

private data class ParserTransition(val value: FSMAlphabet, val state: State)
private class State(private val transitions: MutableList<ParserTransition> = mutableListOf()) {
    companion object {
        fun fromTokens(vararg tokens: TokenEnum): State {
            val root = State()
            var pointer = root

            tokens.forEach {
                val newState = State()
                pointer.addTransition(it, newState)
                pointer = newState
            }

            return root
        }

        fun fromTokens(tokens: List<TokenEnum>) = fromTokens(*tokens.toTypedArray())

        fun fromDSL(dsl: ParserDSL): State {
            val root = State()
            root.addTransition(dsl, State())
            return root
        }

        fun fromAlphabet(vararg characters: FSMAlphabet): State {
            val root = State()
            var pointer = root

            characters.forEach {
                val newState = State()
                when (it) {
                    is FSMAlphabet.AlphabetTokenEnum -> pointer.addTransition(it.tokenEnum, newState)
                    is FSMAlphabet.AlphabetParserDSL -> pointer.addTransition(it.dsl, newState)
                }

                pointer = newState
            }

            return root
        }
    }

    fun addTransition(token: TokenEnum, state: State) {
        transitions.add(ParserTransition(FSMAlphabet.AlphabetTokenEnum(token), state))
    }

    fun addTransition(dsl: ParserDSL, state: State) {
        transitions.add(ParserTransition(FSMAlphabet.AlphabetParserDSL(dsl), state))
    }

    fun next() = if (transitions.isNotEmpty()) transitions[0].value to transitions[0].state else null

    fun traverse(): List<Pair<FSMAlphabet, State>> {
        val list = mutableListOf<Pair<FSMAlphabet, State>>()
        var next = next()

        while (next != null) {
            list.add(next)
            next = next.second.next()
        }

        return list
    }
}

fun parser(name: String, init: ParserDSL.() -> Unit): ParserDSL {
    val parserDSL = ParserDSL(name)
    parserDSL.init()
    return parserDSL
}

val literal = parser("Literal") {
    defOr {
        +Literal.INT
        +Literal.DOUBLE
        +Literal.FLOAT
        +Literal.BOOLEAN
        +Literal.STRING
        +Literal.CHAR
    }
}

fun main(args: Array<String>) {
    println(accessor.toString())
}

val funCall = parser("FunCall") {}
val arrayAccess = parser("ArrayAccess") {}

val accessor = parser("Accessor") {
    def("Identifier") { +Literal.IDENTIFIER }
    def(literal)
    def(funCall)

    def("Dot") {
        +self
        +DOT
        +self
    }

    def("Array") {
        +self
        +arrayAccess
    }
}

val expr = parser("Expr") {
    def(accessor)

    def("Plus") {
        +self
        +PLUS
        +self
    }

    def("Sub") {
        +self
        +SUBTRACT
        +self
    }

    def("Not") {
        +NOT
        +self
    }

    def("Increment") {
        +self
        +INCREMENT
    }

    def("Increment") {
        +INCREMENT
        +self
    }
}

// <expr> ::= <accessor>             #LiteralExpr
//          | <expr> PLUS <expr>     #PlusExpr
//          | <expr> SUBTRACT <expr> #SubExpr
//          | NOT <expr>             #NotExpr
//          | <expr> INCREMENT       #IncrementExpr
//          | INCREMENT <expr>       #IncrementExpr
