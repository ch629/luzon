package com.luzon.parser

import arrow.core.Either
import arrow.core.Left
import arrow.core.Right
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

data class TokenNode<T>(val value: T, val type: Token.TokenEnum) : ASTNode {
    constructor(pair: Pair<T, Token.TokenEnum>) : this(pair.first, pair.second)

    fun isType(tokenType: Token.TokenEnum) = when (tokenType) {
        Token.Literal.DOUBLE -> value is Double
        Token.Literal.FLOAT -> value is Float
        Token.Literal.INT -> value is Int
        Token.Literal.STRING -> value is String && type == Token.Literal.STRING
        Token.Literal.CHAR -> value is Char
        Token.Literal.BOOLEAN -> value is Boolean
        Token.Literal.IDENTIFIER -> value is String && type == Token.Literal.IDENTIFIER
        else -> false
    }
}

data class IdentifierNode(val name: String) : ASTNode
data class VarValNode(val token: Token.TokenEnum) : ASTNode {
    fun isVar() = token == Token.Keyword.VAR
    fun isVal() = token == Token.Keyword.VAL

    companion object {
        fun fromToken(token: Token.TokenEnum): Either<IllegalArgumentException, VarValNode> {
            return when (token) {
                Token.Keyword.VAR,
                Token.Keyword.VAL -> Right(VarValNode(token))
                else -> Left(IllegalArgumentException("Should either be a VAR or a VAL but was a $token"))
            }
        }
    }
}

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

    constructor(name: Token, type: Token) : this(name.data, type.data) //TODO: For temporary testing.
}

data class Parameter(val name: String, val type: String) : ASTNode
data class FunctionDeclaration(val name: String, val parameters: List<Parameter>, val block: FunctionBlock) : ClassStatement

data class Class(val name: String, val block: ClassBlock) : ASTNode

data class IfStatement(val expression: Expression, val block: Block, val elseStatement: Else?) : Statement
data class Else(val block: Block) : ASTNode

data class BinaryExpression(val operator: Token.Symbol, val op1: Expression, val op2: Expression) : Expression
data class UnaryExpression(val operator: Token.Symbol, val op: Expression) : Expression
data class LiteralExpression(val literal: Token) : Expression

data class VariableAccess(val name: String) : Expression

enum class VariableDeclarationType {
    VAR, VAL
}