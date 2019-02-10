package com.luzon.runtime

import com.luzon.rd.ast.ASTNode
import com.luzon.rd.ast.ASTNode.Expression.Binary
import com.luzon.rd.ast.ASTNode.Expression.LiteralExpr
import com.luzon.rd.ast.ASTNodeVisitor
import com.luzon.rd.ast.accept

fun main() {
    val code = Binary.Plus(LiteralExpr.IntLiteral(5), LiteralExpr.IntLiteral(2))

    val c = code.accept(ExpressionVisitor())

    val i = 0
}

class ExpressionVisitor : ASTNodeVisitor<LzObject> {
    private fun accept(node: ASTNode?) = node!!.accept(this)

    override fun visit(node: Binary.Plus): LzObject {
        val left = accept(node.left)
        val right = accept(node.right)

        return if (left.value is Int && right.value is Int)
            LzObject("Int", left.value + right.value)
        else nullObject
    }

    override fun visit(node: Binary.Sub): LzObject {
        val left = accept(node.left)
        val right = accept(node.right)

        return if (left.value is Int && right.value is Int)
            LzObject("Int", left.value - right.value)
        else nullObject
    }

    override fun visit(node: Binary.Mult): LzObject {
        val left = accept(node.left)
        val right = accept(node.right)

        return if (left.value is Int && right.value is Int)
            LzObject("Int", left.value * right.value)
        else nullObject
    }

    override fun visit(node: Binary.Div): LzObject {
        val left = accept(node.left)
        val right = accept(node.right)

        return if (left.value is Int && right.value is Int)
            LzObject("Int", left.value / right.value)
        else nullObject
    }

    override fun visit(node: LiteralExpr.IntLiteral) = LzObject("Int", node.value)
    override fun visit(node: LiteralExpr.FloatLiteral) = LzObject("Float", node.value)
    override fun visit(node: LiteralExpr.DoubleLiteral) = LzObject("Double", node.value)

}