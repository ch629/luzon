package com.luzon.parser

import com.luzon.lexer.Token.Literal
import com.luzon.lexer.Token.TokenEnum
import com.luzon.parser.generator.ClassParameter
import com.luzon.parser.generator.SealedClass
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

    fun paramTypeName() =
            (if (this is FSMAlphabet.AlphabetTokenEnum) tokenEnum.id()!! else name()).capitalize()

    fun isLiteral() = this is FSMAlphabet.AlphabetTokenEnum && tokenEnum is Literal
}

// TODO: I need some sort of way to add a DSL to this lazily, so it only loads the part it needs when it needs it
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

    fun def(token: TokenEnum) {
        addDefinition(FSMAlphabet.AlphabetTokenEnum(token).paramTypeName(), State.fromTokens(token))
    }

    fun defOr(init: ParserDefDSL.() -> Unit) {
        defDsl(init).apply { tokens.forEach { addDefinitionOr(it) } }
    }

    private fun addDefinitionOr(characters: List<FSMAlphabet>) {
        characters.forEach { character ->
            when (character) {
                is FSMAlphabet.AlphabetParserDSL ->
                    addDefinition(character.paramTypeName(), State.fromDSL(character.dsl))
                is FSMAlphabet.AlphabetTokenEnum ->
                    addDefinition(name, character.paramTypeName(), State.fromTokens(character.tokenEnum))
            }
        }
    }

    //region Generator & EBNF Creation

    fun toNewParserGeneratorClass(): SealedClass {
        fun getNewParameters(parameterAlphabet: List<FSMAlphabet>): List<ClassParameter> { // TODO:
            fun alphabetToNewParameter(alphabet: FSMAlphabet, count: Int, allCount: Int = 1): ClassParameter {
                var paramName = alphabet.name().toLowerCase()

                paramName = when (allCount) {
                    1 -> paramName
                    2 -> listOf("left", "right")[count - 1] + paramName.capitalize() // leftExpr, rightExpr?
                    3 -> listOf("x", "y", "z")[count - 1]
                    else -> paramName + count
                }

                return ClassParameter(paramName, alphabet.paramTypeName())
            }

            val paramTypeAllCount = parameterAlphabet.groupBy { it }.mapValues { it.value.size }
            val paramTypeCount = hashMapOf<FSMAlphabet, Int>()

            return parameterAlphabet.asSequence()
                    .filter { it is FSMAlphabet.AlphabetParserDSL || it.isLiteral() }
                    .map {
                        paramTypeCount[it] = (paramTypeCount[it] ?: 0) + 1
                        alphabetToNewParameter(it, paramTypeCount[it]!!, paramTypeAllCount[it]!!)
                    }.toList()
        }

        val sealedClassName = name.capitalize()
        val primaryClass = SealedClass(sealedClassName, superClass = "${sealedClassName}Visitable")
        val subClassNames = mutableSetOf<String>()

        // Sealed Class
        definitions.forEach { (className, rootState) ->

            // Sub Class
            rootState.traverse().forEach { definitions ->
                if (!subClassNames.contains(className)) {
                    subClassNames.add(className)
                    primaryClass.createSubClass(
                            name = className,
                            visitorName = "${sealedClassName}Visitor",
                            parameters = getNewParameters(definitions.map { it.first }))
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

    //endregion
}

class ParserDefDSL(private val forParser: ParserDSL) {
    private var rootState = State()
    private var pointer = rootState

    val tokens get() = rootState.traverse().map { list -> list.map { it.first } }

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

    fun opt(token: TokenEnum) {}
    fun opt(dsl: ParserDSL) {}
    fun opt(dsl: ParserDefDSL.() -> Unit) {}

    val self: ParserDSL get() = forParser
}

//private data class OptionalAlphabet()

// TODO: Some of this may need to be lazy loaded, as non-terminals could potentially have cyclic dependencies
// Lazy loading the DSL for a transition would fix this
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
fun parserOr(name: String, init: ParserDefDSL.() -> Unit) = ParserDSL(name).apply { defOr(init) }