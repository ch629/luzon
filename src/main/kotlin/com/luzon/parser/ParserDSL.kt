package com.luzon.parser

import com.luzon.lexer.Token.Literal
import com.luzon.lexer.Token.Symbol.*
import com.luzon.lexer.Token.TokenEnum
import com.luzon.parser.generator.ParserClass
import com.luzon.parser.generator.ParserParameter
import com.luzon.utils.merge

sealed class FSMAlphabet {
    data class AlphabetTokenEnum(val tokenEnum: TokenEnum) : FSMAlphabet()
    data class AlphabetParserDSL(val dsl: ParserDSL) : FSMAlphabet()

    fun name() = when (this) {
        is AlphabetTokenEnum -> tokenEnum.id()!!.toUpperCase()
        is AlphabetParserDSL -> dsl.name
    }

    fun stringName() = when (this) {
        is AlphabetTokenEnum -> tokenEnum.id()!!.toUpperCase()
        is AlphabetParserDSL -> "<${dsl.name}>"
    }
}

class ParserDSL(val name: String) {
    private val definitions = hashMapOf<String, State>()

    private fun defDsl(init: ParserDefDSL.() -> Unit) = ParserDefDSL(this).apply(init)

    private fun addDefinition(name: String, state: State) {
        if (definitions.containsKey(name)) definitions[name]!!.addTransitions(state)
        else definitions[name] = state
    }

    private fun addDefinition(parserName: String, defName: String, state: State) {
        addDefinition(parserName.capitalize() + defName.capitalize(), state)
    }

    fun def(name: String, init: ParserDefDSL.() -> Unit) {
        val def = defDsl(init)
        addDefinition(name, this.name, State.fromAlphabetList(def.tokens))
    }

    fun def(dsl: ParserDSL) {
        val state = State.fromDSL(dsl)
        addDefinition(dsl.name, name, state)
    }

    fun defOr(init: ParserDefDSL.() -> Unit) {
        defDsl(init).apply { tokens.forEach { addDefinitionOr(it) } }
    }

    private fun addDefinitionOr(characters: List<FSMAlphabet>) {
        characters.forEach { character ->
            when (character) {
                is FSMAlphabet.AlphabetParserDSL ->
                    addDefinition(character.dsl.name,
                            State.fromDSL(character.dsl))
                is FSMAlphabet.AlphabetTokenEnum ->
                    addDefinition(name, character.tokenEnum.id()!!,
                            State.fromTokens(character.tokenEnum))
            }
        }
    }

    fun toParserGeneratorClass(): ParserClass {
        val primaryClass = ParserClass.ParserSealedClass(name.capitalize())

        val subClassNames = mutableSetOf<String>()

        // Sealed Class
        definitions.forEach { (className, rootState) ->

            // Sub Class
            rootState.traverse().forEach { definitions ->
                if (!subClassNames.contains(className)) {
                    subClassNames.add(className)
                    val parameters = mutableListOf<ParserParameter>()
                    val paramCount = hashMapOf<String, Int>()

                    // Parameters
                    definitions.forEach { (param, _) ->
                        val paramType = param.name()
                        if (param is FSMAlphabet.AlphabetParserDSL || (param is FSMAlphabet.AlphabetTokenEnum && param.tokenEnum is Literal)) { // Ignore terminals
                            var paramName = paramType

                            if (definitions.asSequence().filter { it.first.name() == paramName }.count() > 1) { // Contains multiple of the same type
                                if (paramCount.containsKey(paramType))
                                    paramCount[paramType] = paramCount[paramType]!! + 1
                                else paramCount[paramType] = 1

                                paramName += paramCount[paramType]!! // Temp
                            }

                            parameters.add(ParserParameter(paramName, paramType.capitalize()))
                        }
                    }

                    primaryClass.createSubDataClass(className, *parameters.toTypedArray())
                }
            }
        }

        return primaryClass
    }

    override fun toString(): String = StringBuffer().apply {
        val indent = " ".repeat(name.length + 5)
        appendln()
        append("<")
        append(name)
        append(">")
        append(" ::= ")

        val values = definitions.map { (name, tokenStates) ->
            tokenStates.traverse().map {
                name to it.joinToString(" ") { (character, _) ->
                    character.stringName()
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

val literal = parser("literal") {
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
//    println(expr.toString())
    println(accessor.toParserGeneratorClass())
    println(expr.toParserGeneratorClass())
}

val funCall = parser("funCall") {}
val arrayAccess = parser("arrayAccess") {}

val accessor = parser("accessor") {
    def("identifier") { +Literal.IDENTIFIER }
    def(literal)
    def(funCall)

    def("dot") {
        +self
        +DOT
        +self
    }

    def("array") {
        +self
        +arrayAccess
    }
}

val expr = parser("expr") {
    def(accessor)

    def("plus") {
        +self
        +PLUS
        +self
    }

    def("sub") {
        +self
        +SUBTRACT
        +self
    }

    def("not") {
        +NOT
        +self
    }

    def("increment") {
        +self
        +INCREMENT
    }

    def("increment") {
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
