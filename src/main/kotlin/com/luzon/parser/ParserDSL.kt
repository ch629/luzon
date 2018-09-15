package com.luzon.parser

import com.luzon.lexer.Token
import com.luzon.lexer.Token.Literal
import com.luzon.lexer.Token.Symbol.*

class ParserDSL(val name: String) {
    fun def(init: ParserDefDSL.() -> Unit) {}
    fun def(name: String, init: ParserDefDSL.() -> Unit) {}
    fun def(dsl: ParserDSL) {}
    fun defOr(init: ParserDefDSL.() -> Unit) {}
}

class ParserDefDSL {
    operator fun Token.TokenEnum.unaryPlus() {}
    operator fun ParserDSL.unaryPlus() {}

    val self: ParserDSL get() = TODO()
}

fun parser(name: String, init: ParserDSL.() -> Unit): ParserDSL {
    val parserDSL = ParserDSL(name)
    parserDSL.init()
    return parserDSL
}

fun parserDef(name: String, init: ParserDefDSL.() -> Unit): ParserDSL {
    val parserDSL = ParserDSL(name)
    parserDSL.def(init)
    return parserDSL
}

val literal = parser("Literal") {
    defOr {
        +Literal.INT // TODO: Auto name these to LiteralInt? -> parser.name + TokenEnum.name?
        +Literal.DOUBLE
        +Literal.FLOAT
        +Literal.BOOLEAN
        +Literal.STRING
        +Literal.CHAR
    }
}

val funCall = parser("FunCall") {}
val arrayAccess = parser("ArrayAccess") {}

val accessor = parser("Accessor") {
    // TODO: def appends the name to the parser name? i.e. AccessIdentifier or the other way around IdentifierAccess(Makes more sense for Expr)
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
    def("LiteralExpr") {
        +accessor
    }

    def("PlusExpr") {
        +self
        +PLUS
        +self
    }

    def("SubExpr") {
        +self
        +SUBTRACT
        +self
    }

    def("NotExpr") {
        +NOT
        +self
    }

    def("IncrementExpr") {
        +self
        +INCREMENT
    }

    def("IncrementExpr") {
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

