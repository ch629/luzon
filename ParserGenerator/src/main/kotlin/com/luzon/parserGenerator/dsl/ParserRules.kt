package com.luzon.parserGenerator.dsl

import com.luzon.lexer.Token.Literal
import com.luzon.lexer.Token.Symbol.*

val accessor: ParserDSL by lazy {
    parser("accessor") {
        def(Literal.IDENTIFIER)
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
}

val arrayAccess: ParserDSL by lazy { parser("arrayAccess") {} }

val expr: ParserDSL by lazy {
    parser("expr") {
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
}

val funCall: ParserDSL by lazy { parser("funCall") {} }

val literal: ParserDSL by lazy {
    parserOr("literal") {
        +Literal.INT
        +Literal.DOUBLE
        +Literal.FLOAT
        +Literal.BOOLEAN
        +Literal.STRING
        +Literal.CHAR
    }
}

val paramList: ParserDSL by lazy {
    parser("paramList") {
        // TODO: Some of these don't really need names, and can just use a nullable variable
        def("single") {
            +typedVariable
        }

        def("list") {
            +typedVariable
            +COMMA
            +self
        }
    }
}

val typed: ParserDSL by lazy {
    parser("typed") {
        // TODO: I need something so this can just be put at the top, without a name as Typed should be it's own class, or it should be a fragment of a class which is inserted??
        def("") {
            +TYPE
            +Literal.IDENTIFIER
        }
    }
}

val typedVariable: ParserDSL by lazy {
    parser("typedVariable") {
        def("") {
            +Literal.IDENTIFIER
            +typed
        }
    }
}

// <expr> ::= <accessor>             #LiteralExpr
//          | <expr> PLUS <expr>     #PlusExpr
//          | <expr> SUBTRACT <expr> #SubExpr
//          | NOT <expr>             #NotExpr
//          | <expr> INCREMENT       #IncrementExpr
//          | INCREMENT <expr>       #IncrementExpr
