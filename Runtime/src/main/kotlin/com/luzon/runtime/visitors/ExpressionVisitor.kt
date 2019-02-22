package com.luzon.runtime.visitors

import com.luzon.rd.ast.ASTNode
import com.luzon.rd.ast.ASTNode.Expression.Binary
import com.luzon.rd.ast.ASTNode.Expression.LiteralExpr
import com.luzon.rd.expression.ASTNodeVisitor
import com.luzon.rd.expression.accept
import com.luzon.runtime.*

object ExpressionVisitor : ASTNodeVisitor<LzObject> {
    private var environment = Environment.global
    private fun accept(node: ASTNode?) = node?.accept(this) ?: nullObject // TODO: GlobalVisitor?

    private fun Any.isNumerical() = weight() != -1

    private fun Any.weight(): Int = when (this) {
        is Int -> 0
        is Float -> 1
        is Double -> 2
        else -> -1
    }

    private fun Any.asFloat(): Float = when (this) {
        is Int -> toFloat()
        is Float -> this
        is Double -> toFloat()
        else -> -1f
    }

    private fun Any.asDouble(): Double = when (this) {
        is Int -> toDouble()
        is Float -> toDouble()
        is Double -> this
        else -> -1.0
    }

    // TODO: If I can get ArrowKt working without it freezing IntelliJ, rather than else -> nullObject, I can return Left("Expected ... got ..."), or a custom error object.
    private fun subtract(left: Any, right: Any) = when (maxOf(left, right, compareBy { it.weight() })) {
        is Int -> LzObject(LzInt, left as Int - right as Int)
        is Float -> LzObject(LzFloat, left.asFloat() - right.asFloat())
        is Double -> LzObject(LzDouble, right.asDouble() - right.asDouble())
        else -> nullObject
    }

    private fun plus(left: Any, right: Any) = when (maxOf(left, right, compareBy { it.weight() })) {
        is Int -> LzObject(LzInt, left as Int + right as Int)
        is Float -> LzObject(LzFloat, left.asFloat() + right.asFloat())
        is Double -> LzObject(LzDouble, right.asDouble() + right.asDouble())
        else -> nullObject
    }

    private fun multiply(left: Any, right: Any) = when (maxOf(left, right, compareBy { it.weight() })) {
        is Int -> LzObject(LzInt, left as Int * right as Int)
        is Float -> LzObject(LzFloat, left.asFloat() * right.asFloat())
        is Double -> LzObject(LzDouble, right.asDouble() * right.asDouble())
        else -> nullObject
    }

    private fun divide(left: Any, right: Any) = when (maxOf(left, right, compareBy { it.weight() })) {
        is Int -> LzObject(LzInt, left as Int / right as Int)
        is Float -> LzObject(LzFloat, left.asFloat() / right.asFloat())
        is Double -> LzObject(LzDouble, right.asDouble() / right.asDouble())
        else -> nullObject
    }

    override fun visit(node: Binary.Plus): LzObject {
        val left = accept(node.left)
        val right = accept(node.right)

        return when {
            left.value.isNumerical() && right.value.isNumerical() ->
                plus(accept(node.left).value, accept(node.right).value)
            left.value is String && right.value is String ->
                LzObject(LzString, left.value + right.value)
            else -> nullObject
        }
    }

    override fun visit(node: Binary.Sub) = subtract(accept(node.left).value, accept(node.right).value)
    override fun visit(node: Binary.Mult) = multiply(accept(node.left).value, accept(node.right).value)
    override fun visit(node: Binary.Div) = divide(accept(node.left).value, accept(node.right).value)

    override fun visit(node: LiteralExpr.IntLiteral) = LzObject(LzInt, node.value)
    override fun visit(node: LiteralExpr.FloatLiteral) = LzObject(LzFloat, node.value)
    override fun visit(node: LiteralExpr.DoubleLiteral) = LzObject(LzDouble, node.value)
    override fun visit(node: LiteralExpr.BooleanLiteral) = LzObject(LzBoolean, node.value)

    override fun visit(node: LiteralExpr.IdentifierLiteral) = environment[node.name] ?: nullObject

    override fun visit(node: Binary.Equals) = LzObject(LzBoolean, accept(node.left).value == accept(node.right).value)
    override fun visit(node: Binary.NotEquals) = LzObject(LzBoolean, accept(node.left).value != accept(node.right).value)

    // Currently just converting all numerical types to floats to compare, as this won't lose any accuracy,
    // but this will be more resource consuming.
    // TODO: Check they are all numerical first, else error
    override fun visit(node: Binary.GreaterEquals) = LzObject(LzBoolean, accept(node.left).asFloat() >= accept(node.right).asFloat())

    override fun visit(node: Binary.Greater) = LzObject(LzBoolean, accept(node.left).asFloat() > accept(node.right).asFloat())
    override fun visit(node: Binary.Less) = LzObject(LzBoolean, accept(node.left).asFloat() < accept(node.right).asFloat())
    override fun visit(node: Binary.LessEquals) = LzObject(LzBoolean, accept(node.left).asFloat() <= accept(node.right).asFloat())

    override fun visit(node: Binary.And): LzObject {
        val left = accept(node.left)
        val right = accept(node.right)

        if (left.value is Boolean && right.value is Boolean)
            return LzObject(LzBoolean, left.value && right.value)
        return nullObject
    }

    override fun visit(node: Binary.Or): LzObject {
        val left = accept(node.left)
        val right = accept(node.right)

        if (left.value is Boolean && right.value is Boolean)
            return LzObject(LzBoolean, left.value || right.value)
        return nullObject
    }

    override fun visit(node: ASTNode.Expression.Unary.Sub) = accept(node.expr).run {
        when (value) {
            is Int -> LzObject(LzInt, -value)
            is Float -> LzObject(LzFloat, -value)
            is Double -> LzObject(LzDouble, -value)
            else -> nullObject
        }
    }

    override fun visit(node: ASTNode.Expression.Unary.Not) = accept(node.expr).run {
        if (value is Boolean) LzObject(LzBoolean, !value)
        else nullObject
    }
}