package com.luzon.parser

import com.luzon.lexer.Token

//TODO: Most of this will need deleting, and be replaced by generated classes at a later date.
interface ASTNode

interface Statement : ASTNode
interface ClassStatement : Statement
interface Expression : ASTNode

object NullNode : ASTNode

data class ClassBlock(val statements: List<ClassStatement>) : ASTNode
data class FunctionBlock(val statement: List<Statement>) : ASTNode
data class Block(val statements: List<Statement>) : ASTNode

data class Argument(val expression: Expression) : ASTNode
data class FunctionCall(val name: String, val arguments: List<Argument>) : Statement, Expression
data class Return(val expression: Expression) : Statement

class VariableDeclaration : Statement {
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

    constructor(id: IdentifierNode, assign: Expression) : this(id.name, assign)

    constructor(name: String, type: String, assign: Expression) {
        this.name = name
        this.type = type
        this.assign = assign
    }
}

data class Parameter(val name: String, val type: String) : ASTNode
data class FunctionDeclaration(val name: String, val parameters: List<Parameter>, val block: FunctionBlock) : ClassStatement //TODO: Default parameter values?

data class Class(val name: String, val block: ClassBlock) : ASTNode //TODO: Constructors, Inheritance

data class IfStatement(val expression: Expression, val block: Block, val elseStatement: Else?) : Statement
data class Else(val block: Block) : ASTNode

data class BinaryExpression(val operator: Token.Symbol, val op1: Expression, val op2: Expression) : Expression
data class UnaryExpression(val operator: Token.Symbol, val op: Expression) : Expression
data class LiteralExpression(val literal: Token) : Expression

data class VariableAccess(val name: String) : Expression

enum class VariableDeclarationType {
    VAR, VAL
}

sealed class Color
object Blue : Color()
object Orange : Color()

fun main(args: Array<String>) {
    val color: Color = Blue

    Test.accept(color)
}

object Test { //Visitor Pattern
    fun accept(color: Color): Unit = when (color) {
        is Blue -> accept(color)
        is Orange -> accept(color)
    }

    fun accept(blue: Blue) {
        println("BLUE")
    }

    fun accept(orange: Orange) {
        println("ORANGE")
    }
}