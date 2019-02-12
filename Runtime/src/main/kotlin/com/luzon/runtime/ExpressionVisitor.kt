package com.luzon.runtime

import com.luzon.rd.ast.ASTNode
import com.luzon.rd.ast.ASTNode.Expression.Binary
import com.luzon.rd.ast.ASTNode.Expression.LiteralExpr
import com.luzon.rd.ast.ASTNodeVisitor
import com.luzon.rd.ast.accept

object ExpressionVisitor : ASTNodeVisitor<LzObject> {
    private fun accept(node: ASTNode?) = node!!.accept(this)

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

    private fun sub(left: Any, right: Any) = when (maxOf(left, right, compareBy { it.weight() })) {
        is Int -> LzObject("Int", left as Int - right as Int)
        is Float -> LzObject("Float", left.asFloat() - right.asFloat())
        is Double -> LzObject("Double", right.asDouble() - right.asDouble())
        else -> nullObject
    }

    private fun plus(left: Any, right: Any) = when (maxOf(left, right, compareBy { it.weight() })) {
        is Int -> LzObject("Int", left as Int + right as Int)
        is Float -> LzObject("Float", left.asFloat() + right.asFloat())
        is Double -> LzObject("Double", right.asDouble() + right.asDouble())
        else -> nullObject
    }

    private fun mult(left: Any, right: Any) = when (maxOf(left, right, compareBy { it.weight() })) {
        is Int -> LzObject("Int", left as Int * right as Int)
        is Float -> LzObject("Float", left.asFloat() * right.asFloat())
        is Double -> LzObject("Double", right.asDouble() * right.asDouble())
        else -> nullObject
    }

    private fun div(left: Any, right: Any) = when (maxOf(left, right, compareBy { it.weight() })) {
        is Int -> LzObject("Int", left as Int / right as Int)
        is Float -> LzObject("Float", left.asFloat() / right.asFloat())
        is Double -> LzObject("Double", right.asDouble() / right.asDouble())
        else -> nullObject
    }

    override fun visit(node: Binary.Plus): LzObject {
        val left = accept(node.left)
        val right = accept(node.right)

        return when {
            left.value.isNumerical() && right.value.isNumerical() ->
                plus(accept(node.left).value, accept(node.right).value)
            left.value is String && right.value is String ->
                LzObject("String", left.value + right.value)
            else -> nullObject
        }
    }

    override fun visit(node: Binary.Sub) = sub(accept(node.left).value, accept(node.right).value)
    override fun visit(node: Binary.Mult) = mult(accept(node.left).value, accept(node.right).value)
    override fun visit(node: Binary.Div) = div(accept(node.left).value, accept(node.right).value)

    override fun visit(node: LiteralExpr.IntLiteral) = LzObject("Int", node.value)
    override fun visit(node: LiteralExpr.FloatLiteral) = LzObject("Float", node.value)
    override fun visit(node: LiteralExpr.DoubleLiteral) = LzObject("Double", node.value)
}