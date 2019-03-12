package com.luzon.runtime.visitors

import com.luzon.rd.ast.ASTNode
import com.luzon.rd.ast.ASTNode.Expression.*
import com.luzon.rd.expression.ASTNodeVisitor
import com.luzon.rd.expression.accept
import com.luzon.runtime.*

object ExpressionVisitor : ASTNodeVisitor<LzObject> {
    private fun accept(node: ASTNode?) = node?.accept(this) ?: nullObject

    private fun Any?.isNumerical() = this != null && weight() != -1 && weight() != 3

    private fun Any.weight(): Int = when (this) {
        is Int -> 0
        is Float -> 1
        is Double -> 2
        is String -> 3
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

    private fun Any.asString(): String = when (this) {
        is Int -> toString()
        is Float -> toString()
        is Double -> toString()
        is String -> this
        else -> ""
    }

    private fun subtract(left: Any?, right: Any?) = if (right != null && left != null)
        when (maxOf(left, right, compareBy { it.weight() })) {
            is Int -> primitiveObject(left as Int - right as Int)
            is Float -> primitiveObject(left.asFloat() - right.asFloat())
            is Double -> primitiveObject(left.asDouble() - right.asDouble())
            else -> nullObject
        } else nullObject

    private fun plus(left: Any?, right: Any?) = if (right != null && left != null)
        when (maxOf(left, right, compareBy { it.weight() })) {
            is Int -> primitiveObject(left as Int + right as Int)
            is Float -> primitiveObject(left.asFloat() + right.asFloat())
            is Double -> primitiveObject(left.asDouble() + right.asDouble())
            is String -> primitiveObject(left.asString() + right.asString())
            else -> nullObject
        } else nullObject

    private fun multiply(left: Any?, right: Any?) = if (right != null && left != null)
        when (maxOf(left, right, compareBy { it.weight() })) {
            is Int -> primitiveObject(left as Int * right as Int)
            is Float -> primitiveObject(left.asFloat() * right.asFloat())
            is Double -> primitiveObject(left.asDouble() * right.asDouble())
            else -> nullObject
        } else nullObject

    private fun divide(left: Any?, right: Any?) = if (right != null && left != null)
        when (maxOf(left, right, compareBy { it.weight() })) {
            is Int -> primitiveObject(left as Int / right as Int)
            is Float -> primitiveObject(left.asFloat() / right.asFloat())
            is Double -> primitiveObject(left.asDouble() / right.asDouble())
            else -> nullObject
        } else nullObject

    override fun visit(node: Binary.Plus) = plus(accept(node.left).value, accept(node.right).value)
    override fun visit(node: Binary.Sub) = subtract(accept(node.left).value, accept(node.right).value)
    override fun visit(node: Binary.Mult) = multiply(accept(node.left).value, accept(node.right).value)
    override fun visit(node: Binary.Div) = divide(accept(node.left).value, accept(node.right).value)

    override fun visit(node: LiteralExpr.IntLiteral) = primitiveObject(node.value)
    override fun visit(node: LiteralExpr.FloatLiteral) = primitiveObject(node.value)
    override fun visit(node: LiteralExpr.DoubleLiteral) = primitiveObject(node.value)
    override fun visit(node: LiteralExpr.BooleanLiteral) = primitiveObject(node.value)
    override fun visit(node: LiteralExpr.StringLiteral) = primitiveObject(node.value)

    override fun visit(node: LiteralExpr.IdentifierLiteral) = EnvironmentManager[node.name] ?: nullObject

    override fun visit(node: Binary.Equals) = primitiveObject(accept(node.left).value == accept(node.right).value)
    override fun visit(node: Binary.NotEquals) = primitiveObject(accept(node.left).value != accept(node.right).value)

    // Currently just converting all numerical types to floats to compare, as this won't lose any accuracy,
    // but this will be more resource consuming.
    // TODO: Check they are all numerical first, else error
    override fun visit(node: Binary.GreaterEquals) = primitiveObject(accept(node.left).asFloat() >= accept(node.right).asFloat())

    override fun visit(node: Binary.Greater) = primitiveObject(accept(node.left).asFloat() > accept(node.right).asFloat())
    override fun visit(node: Binary.Less) = primitiveObject(accept(node.left).asFloat() < accept(node.right).asFloat())
    override fun visit(node: Binary.LessEquals) = primitiveObject(accept(node.left).asFloat() <= accept(node.right).asFloat())

    private data class BinaryObjects(val left: LzObject, val right: LzObject)

    private fun acceptBinary(node: Binary) = BinaryObjects(accept(node.left), accept(node.right))

    override fun visit(node: Binary.And): LzObject = acceptBinary(node).run {
        if (left.value is Boolean && right.value is Boolean)
            return primitiveObject(left.value && right.value)
        return nullObject
    }

    override fun visit(node: Binary.Or): LzObject = acceptBinary(node).run {
        if (left.value is Boolean && right.value is Boolean)
            return primitiveObject(left.value || right.value)
        return nullObject
    }

    override fun visit(node: Unary.Sub) = accept(node.expr).run {
        when (value) {
            is Int -> primitiveObject(-value)
            is Float -> primitiveObject(-value)
            is Double -> primitiveObject(-value)
            else -> nullObject
        }
    }

    override fun visit(node: Unary.Not) = accept(node.expr).run {
        if (value is Boolean) primitiveObject(!value)
        else nullObject
    }

    override fun visit(node: ASTNode.OperatorVariableAssign): LzObject {
        val (name, expr, op) = node
        val binaryExpr = Binary.fromOperator(op, LiteralExpr.IdentifierLiteral(name), expr)
        val obj = accept(binaryExpr)

        EnvironmentManager[name] = obj // TODO: Type checking on this

        return obj
    }

    private fun unaryModifier(node: Unary): LzObject {
        val increment = node is Unary.Increment
        val pre = (node as? Unary.Increment)?.pre ?: (node as? Unary.Decrement)?.pre ?: false
        val exprObj = accept(node.expr)

        if (node.expr is LiteralExpr.IdentifierLiteral) {
            val newObject = if (exprObj.value!!.isNumerical()) { // TODO: Check null here
                when (exprObj.value) {
                    is Int -> primitiveObject(exprObj.value + if (increment) 1 else -1)
                    is Float -> primitiveObject(exprObj.value + if (increment) 1 else -1)
                    is Double -> primitiveObject(exprObj.value + if (increment) 1 else -1)
                    else -> nullObject
                }
            } else nullObject

            EnvironmentManager[(node.expr as LiteralExpr.IdentifierLiteral).name] = newObject

            return if (pre) exprObj else newObject
        }

        return nullObject
    }

    override fun visit(node: Unary.Increment) = unaryModifier(node)
    override fun visit(node: Unary.Decrement) = unaryModifier(node)

    override fun visit(node: LiteralExpr.FunctionCall): LzObject {
        val params = node.params.map { it.accept(ExpressionVisitor) }

        return EnvironmentManager(node.name, params)
    }

    override fun visit(node: LiteralExpr.DotChainLiteral): LzObject {
        var last: LzObject = node.value.accept(ExpressionVisitor)

        if (node.next != null) {
            withEnvironment(last.environment) {
                last = visit(node.next!!)
            }
        }

        return last
    }
}