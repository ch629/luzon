package com.luzon.recursive_descent.ast

import com.luzon.lexer.Token
import com.luzon.lexer.Token.Literal
import com.luzon.lexer.Token.Symbol.AND
import com.luzon.lexer.Token.Symbol.DIVIDE
import com.luzon.lexer.Token.Symbol.EQUAL_EQUAL
import com.luzon.lexer.Token.Symbol.GREATER
import com.luzon.lexer.Token.Symbol.GREATER_EQUAL
import com.luzon.lexer.Token.Symbol.LESS
import com.luzon.lexer.Token.Symbol.LESS_EQUAL
import com.luzon.lexer.Token.Symbol.MODULUS
import com.luzon.lexer.Token.Symbol.MULTIPLY
import com.luzon.lexer.Token.Symbol.NOT_EQUAL
import com.luzon.lexer.Token.Symbol.OR
import com.luzon.lexer.Token.Symbol.PLUS
import com.luzon.lexer.Token.Symbol.SUBTRACT

sealed class SyntaxTreeNode {
    data class Class(val name: String, val constructor: Constructor?, val block: Block) : SyntaxTreeNode()

    data class Constructor(val variables: List<ConstructorVariableDeclaration>) : SyntaxTreeNode() // TODO: This is the Primary Constructor

    // TODO: Not all of these should be val/var, only if they are held within the class
    data class ConstructorVariableDeclaration(val name: String, val type: String, val constant: Boolean) : SyntaxTreeNode()

    data class FunctionDefinition(val name: String, val parameters: List<FunctionParameter>, val returnType: String?, val block: Block) : SyntaxTreeNode()
    data class FunctionParameter(val name: String, val type: String) : SyntaxTreeNode()

    data class ForLoop(val id: String, val start: Int, val end: Int, val block: Block) : SyntaxTreeNode() // TODO: Basic for loop for now.
    data class WhileLoop(val doWhile: Boolean, val expr: Expression, val block: Block) : SyntaxTreeNode()

    data class IfStatement(val expr: Expression, val block: Block, val elseStatement: ElseStatements?) : SyntaxTreeNode()

    data class Return(val data: Expression?) : SyntaxTreeNode()

    sealed class ElseStatements : SyntaxTreeNode() {
        data class ElseIfStatement(val ifStatement: IfStatement) : ElseStatements()
        data class ElseStatement(val block: Block) : ElseStatements()
    }

    data class VariableDeclaration(val name: String, val type: String?, val expr: Expression, val constant: Boolean) : SyntaxTreeNode()
    data class VariableAssign(val name: String, val expr: Expression) : SyntaxTreeNode()

    data class OperatorVariableAssign(val name: String, val expr: Expression, val operator: Token.Symbol) : SyntaxTreeNode()

    data class Block(val nodes: List<SyntaxTreeNode>) : SyntaxTreeNode()

    sealed class Expression : SyntaxTreeNode() {
        sealed class Binary(var left: Expression?, var right: Expression?) : Expression() { // TODO: Mod, is?
            companion object {
                fun fromOperator(symbol: Token.TokenEnum?, left: Expression?, right: Expression?) = when (symbol) {
                    PLUS -> Expression.Binary::Plus
                    SUBTRACT -> Expression.Binary::Subtract
                    MULTIPLY -> Expression.Binary::Multiply
                    DIVIDE -> Expression.Binary::Divide
                    MODULUS -> Expression.Binary::Modulus

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
            class Subtract(left: Expression? = null, right: Expression? = null) : Binary(left, right)
            class Multiply(left: Expression? = null, right: Expression? = null) : Binary(left, right)
            class Divide(left: Expression? = null, right: Expression? = null) : Binary(left, right)
            class Modulus(left: Expression? = null, right: Expression? = null) : Binary(left, right)

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
            class Minus(expr: Expression? = null) : Unary(expr)
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
                    Literal.STRING -> StringLiteral(token.data)
                    Literal.CHAR -> CharLiteral(token.data[0])
                    else -> null
                }
            }

            data class IntLiteral(val value: Int) : LiteralExpr()
            data class FloatLiteral(val value: Float) : LiteralExpr()
            data class DoubleLiteral(val value: Double) : LiteralExpr()
            data class IdentifierLiteral(val name: String) : LiteralExpr()
            data class BooleanLiteral(val value: Boolean) : LiteralExpr()
            data class StringLiteral(val value: String) : LiteralExpr()
            data class CharLiteral(val value: Char) : LiteralExpr()
            data class FunctionCall(val name: String, val params: List<Expression>) : LiteralExpr()

            data class DotChainLiteral(val value: Expression, val next: DotChainLiteral? = null) : LiteralExpr()
        }
    }
}
