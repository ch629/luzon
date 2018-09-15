package com.luzon.parser

import com.luzon.lexer.Token.Literal
import com.luzon.lexer.Token.Symbol.*
import com.luzon.lexer.Token.TokenEnum

class ParserDSL(val name: String) {
    val tokenDefinitions = hashMapOf<String, List<TokenEnum>>()

    private fun defDsl(init: ParserDefDSL.() -> Unit): ParserDefDSL {
        val def = ParserDefDSL(this)
        def.init()
        return def
    }

    fun def(name: String, init: ParserDefDSL.() -> Unit) {
        val def = defDsl(init)
        tokenDefinitions[this.name + name] = def.tokens
    }

    fun def(dsl: ParserDSL) {

    }

    fun defOr(init: ParserDefDSL.() -> Unit) {
        val def = defDsl(init)

        def.tokens.forEach {
            tokenDefinitions[this.name + it.id()!!.capitalize()] = listOf(it)
        }
    }

    override fun toString(): String = StringBuffer().apply {
        val indent = " ".repeat(name.length + 3)
        append(name)
        append(" ::= ")

        val values = tokenDefinitions.map { (name, tokenList) ->
            name to tokenList.joinToString { it.id()!!.toUpperCase() }
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
    var tokens = emptyList<TokenEnum>()
        private set

    operator fun TokenEnum.unaryPlus() {
        tokens += this
    }

    operator fun ParserDSL.unaryPlus() {}

    val self: ParserDSL get() = forParser
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
    println(literal.toString())
}

val funCall = parser("FunCall") {}
val arrayAccess = parser("ArrayAccess") {}

val accessor = parser("Accessor") {
    def("Identifier") { +Literal.IDENTIFIER }
    def(literal) // TODO: for non-terminals we can probably just use the literal.name
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
    def("Literal") {
        +accessor
    }

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

// TODO: Two of the same (Should work with just reduceSkip(2) { it.tokenEnum != Token.Symbol.INCREMENT }
// -> This would be a problem if they were slightly different, rather than just the reverse of each other

