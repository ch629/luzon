package com.luzon.parser

import com.luzon.lexer.Token.Literal
import com.luzon.lexer.Token.Symbol.*
import com.luzon.lexer.Token.TokenEnum
import com.luzon.utils.merge

sealed class FSMAlphabet {
    data class AlphabetTokenEnum(val tokenEnum: TokenEnum) : FSMAlphabet()
    data class AlphabetParserDSL(val dsl: ParserDSL) : FSMAlphabet()

    override fun toString() = when (this) {
        is AlphabetTokenEnum -> tokenEnum.id()!!.toUpperCase()
        is AlphabetParserDSL -> dsl.name
    }

    fun name() = when (this) {
        is AlphabetParserDSL -> dsl.name.decapitalize()
        is AlphabetTokenEnum -> tokenEnum.id()!!.toUpperCase()
    }
}

class ParserDSL(val name: String) {
    private val definitions = hashMapOf<String, State>()

    private fun defDsl(init: ParserDefDSL.() -> Unit) = ParserDefDSL(this).apply(init)

    private fun addDefinition(name: String, state: State) {
        if (definitions.containsKey(name)) definitions[name]!!.addTransitions(state)
        else definitions[name] = state
    }

    fun def(name: String, init: ParserDefDSL.() -> Unit) {
        val def = defDsl(init)
        addDefinition(name + this.name, State.fromAlphabetList(def.tokens))
    }

    fun def(dsl: ParserDSL) {
        State.fromDSL(dsl).apply { addDefinition(dsl.name, this) }
    }

    fun defOr(init: ParserDefDSL.() -> Unit) {
        defDsl(init).apply { tokens.forEach { addDefinitionOr(it) } }
    }

    private fun addDefinitionOr(characters: List<FSMAlphabet>) {
        characters.forEach { character ->
            when (character) {
                is FSMAlphabet.AlphabetParserDSL ->
                    addDefinition(character.dsl.name.decapitalize(),
                            State.fromDSL(character.dsl))
                is FSMAlphabet.AlphabetTokenEnum ->
                    addDefinition(name + character.tokenEnum.id()!!.capitalize(),
                            State.fromTokens(character.tokenEnum))
            }
        }
    }

    override fun toString(): String = StringBuffer().apply {
        val indent = " ".repeat(name.length + 3)
        appendln()
        append(name)
        append(" ::= ")

        val values = definitions.map { (name, tokenStates) ->
            tokenStates.traverse().map {
                name to it.joinToString(" ") { (character, _) ->
                    character.name()
                }
            }
        }.merge()

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

    val tokens get() = rootState.traverse().map { it.map { it.first } }

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
        fun fromTokens(vararg tokens: TokenEnum) = State().apply {
            var pointer = this

            tokens.forEach {
                val newState = State()
                pointer.addTransition(it, newState)
                pointer = newState
            }
        }

        fun fromTokens(tokens: List<TokenEnum>) = fromTokens(*tokens.toTypedArray())

        fun fromDSL(dsl: ParserDSL) = State().apply {
            addTransition(dsl, State())
        }

        fun fromAlphabetList(lists: List<List<FSMAlphabet>>) = State().apply {
            lists.forEach { addTransitions(fromAlphabet(*it.toTypedArray())) }
        }

        fun fromAlphabet(vararg characters: FSMAlphabet) = State().apply {
            var pointer = this

            characters.forEach {
                val newState = State()
                when (it) {
                    is FSMAlphabet.AlphabetTokenEnum -> pointer.addTransition(it.tokenEnum, newState)
                    is FSMAlphabet.AlphabetParserDSL -> pointer.addTransition(it.dsl, newState)
                }

                pointer = newState
            }
        }
    }

    fun addTransition(token: TokenEnum, state: State) {
        transitions.add(ParserTransition(FSMAlphabet.AlphabetTokenEnum(token), state))
    }

    fun addTransition(dsl: ParserDSL, state: State) {
        transitions.add(ParserTransition(FSMAlphabet.AlphabetParserDSL(dsl), state))
    }

    fun addTransitions(state: State) {
        // Assuming that all states will start with a root, we can just add the root states
        transitions.addAll(state.transitions)
    }

    fun next() = if (transitions.isNotEmpty()) transitions[0].value to transitions[0].state else null

    fun traverse() = transitions.map {
        mutableListOf(it.value to it.state).apply {
            var next = it.state.next()

            while (next != null) {
                add(next)
                next = next.second.next()
            }
        }
    }
}

fun parser(name: String, init: ParserDSL.() -> Unit) = ParserDSL(name).apply(init)

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
    println(expr.toString())
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
