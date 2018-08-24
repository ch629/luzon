package com.luzon.parser

import com.luzon.lexer.Symbol
import com.luzon.lexer.Token

interface Statement
interface ClassStatement : Statement
interface Expression

data class ClassBlock(val statements: List<ClassStatement>)
data class FunctionBlock(val statement: List<Statement>)
data class Block(val statements: List<Statement>)

data class Argument(val expression: Expression)
data class FunctionCall(val name: String, val arguments: List<Argument>) : Statement, Expression
data class Return(val expression: Expression) : Statement

class VariableDeclarataion : Statement {
    val name: String
    val type: String?
    val assign: Expression?

    constructor(name: String, type: String) {
        this.name = name
        this.type = type
        this.assign = null
    }

    constructor(name: String, assign: Expression) {
        this.name = name
        this.type = null
        this.assign = assign
    }

    constructor(name: String, type: String, assign: Expression) {
        this.name = name
        this.type = type
        this.assign = assign
    }
}

data class Parameter(val name: String, val type: String)
data class FunctionDeclaration(val name: String, val parameters: List<Parameter>, val block: FunctionBlock) : ClassStatement //TODO: Default parameter values?

data class Class(val name: String, val block: ClassBlock) //TODO: Constructors, Inheritance

data class IfStatement(val expression: Expression, val block: Block, val elseStatement: Else?) : Statement
data class Else(val block: Block)

data class BinaryExpression(val operator: Symbol, val op1: Expression, val op2: Expression) : Expression
data class UnaryExpression(val operator: Symbol, val op: Expression) : Expression
data class LiteralExpression(val literal: Token) : Expression

data class VariableAccess(val name: String) : Expression

enum class VariableDeclarationType {
    VAR, VAL
}