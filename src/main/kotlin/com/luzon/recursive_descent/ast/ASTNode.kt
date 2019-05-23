package com.luzon.recursive_descent.ast

import com.luzon.lexer.Token
import com.luzon.lexer.Token.Literal
import com.luzon.lexer.Token.Symbol.*

sealed class ASTNode {
    data class Class(val name: String, val constructor: Constructor?, val block: Block) : ASTNode()

    data class Constructor(val variables: List<ConstructorVariableDeclaration>) : ASTNode() // TODO: This is the Primary Constructor
    // TODO: Not all of these should be val/var, only if they are held within the class
    data class ConstructorVariableDeclaration(val name: String, val type: String, val constant: Boolean) : ASTNode()

    data class FunctionDefinition(val name: String, val parameters: List<FunctionParameter>, val returnType: String?, val block: Block) : ASTNode()
    data class FunctionParameter(val name: String, val type: String) : ASTNode()

    data class ForLoop(val id: String, val start: Int, val end: Int, val block: Block) : ASTNode() // TODO: Basic for loop for now.
    data class WhileLoop(val doWhile: Boolean, val expr: Expression, val block: Block) : ASTNode()

    data class IfStatement(val expr: Expression, val block: Block, val elseStatement: ElseStatements?) : ASTNode()

    data class Return(val data: Expression?) : ASTNode()

    sealed class ElseStatements : ASTNode() {
        data class ElseIfStatement(val ifStatement: IfStatement) : ElseStatements()
        data class ElseStatement(val block: Block) : ElseStatements()
    }

    data class VariableDeclaration(val name: String, val type: String?, val expr: Expression, val constant: Boolean) : ASTNode()
    data class VariableAssign(val name: String, val expr: Expression) : ASTNode()

    data class OperatorVariableAssign(val name: String, val expr: Expression, val operator: Token.Symbol) : ASTNode()

    data class Block(val nodes: List<ASTNode>) : ASTNode()

    sealed class Expression : ASTNode() {
        sealed class Binary(var left: Expression?, var right: Expression?) : Expression() { // TODO: Mod, is?
            companion object {
                fun fromOperator(symbol: Token.TokenEnum?, left: Expression?, right: Expression?) = when (symbol) {
                    PLUS -> Expression.Binary::Plus
                    SUBTRACT -> Expression.Binary::Sub
                    MULTIPLY -> Expression.Binary::Mult
                    DIVIDE -> Expression.Binary::Div
                    MOD -> Expression.Binary::Mod

                    EQUAL_EQUAL -> Expression.Binary::Equals
                    NOT_EQUAL -> Expression.Binary::NotEquals
                    GREATER_EQUAL -> Expression.Binary::GreaterEquals
                    GREATER -> Expression.Binary::Greater
                    LESS -> Expression.Binary::Less
                    LESS_EQUAL -> Expression.Binary::LessEquals
                    AND -> Expression.Binary::And
                    OR -> Expression.Binary::Or
                    else -> null
                }?.invoke(left, right)
            }

            class Plus(left: Expression? = null, right: Expression? = null) : Binary(left, right)
            class Sub(left: Expression? = null, right: Expression? = null) : Binary(left, right)
            class Mult(left: Expression? = null, right: Expression? = null) : Binary(left, right)
            class Div(left: Expression? = null, right: Expression? = null) : Binary(left, right)
            class Mod(left: Expression? = null, right: Expression? = null) : Binary(left, right)

            class Equals(left: Expression? = null, right: Expression? = null) : Binary(left, right)
            class NotEquals(left: Expression? = null, right: Expression? = null) : Binary(left, right)
            class GreaterEquals(left: Expression? = null, right: Expression? = null) : Binary(left, right)
            class Greater(left: Expression? = null, right: Expression? = null) : Binary(left, right)
            class Less(left: Expression? = null, right: Expression? = null) : Binary(left, right)
            class LessEquals(left: Expression? = null, right: Expression? = null) : Binary(left, right)

            class And(left: Expression? = null, right: Expression? = null) : Binary(left, right)
            class Or(left: Expression? = null, right: Expression? = null) : Binary(left, right)
        }

        sealed class Unary(var expr: Expression?) : Expression() {
            class Sub(expr: Expression? = null) : Unary(expr)
            class Not(expr: Expression? = null) : Unary(expr)

            class Increment(expr: Expression? = null, val pre: Boolean) : Unary(expr)
            class Decrement(expr: Expression? = null, val pre: Boolean) : Unary(expr)
        }

        sealed class LiteralExpr : Expression() {
            companion object {
                fun fromToken(token: Token) = when (token.tokenEnum) {
                    Literal.INT -> IntLiteral(token.data.toInt())
                    Literal.FLOAT -> FloatLiteral(token.data.toFloat())
                    Literal.DOUBLE -> DoubleLiteral(token.data.toDouble())
                    Literal.BOOLEAN -> BooleanLiteral(token.data.toBoolean())
                    Literal.IDENTIFIER -> IdentifierLiteral(token.data)
                    Literal.STRING -> StringLiteral(token.data.substring(1, token.data.length - 1))
                    else -> null
                }
            }

            data class IntLiteral(val value: Int) : LiteralExpr()
            data class FloatLiteral(val value: Float) : LiteralExpr()
            data class DoubleLiteral(val value: Double) : LiteralExpr()
            data class IdentifierLiteral(val name: String) : LiteralExpr()
            data class BooleanLiteral(val value: Boolean) : LiteralExpr()
            data class StringLiteral(val value: String) : LiteralExpr()
            data class FunctionCall(val name: String, val params: List<Expression>) : LiteralExpr()

            data class DotChainLiteral(val value: Expression, val next: DotChainLiteral? = null) : LiteralExpr()
        }
    }
}