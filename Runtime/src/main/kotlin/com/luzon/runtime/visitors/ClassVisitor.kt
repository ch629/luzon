package com.luzon.runtime.visitors

import com.luzon.rd.ast.ASTNode
import com.luzon.rd.expression.ASTNodeVisitor
import com.luzon.rd.expression.accept

object ClassVisitor : ASTNodeVisitor<Any> {
    private fun accept(node: ASTNode?) {
        if (node is ASTNode.Expression) node.accept(ExpressionVisitor)
        else node?.accept(this)
    }

    override fun visit(node: ASTNode.Class) {
        val (name, constructor, block) = node

        accept(constructor) // TODO: Need to add these to the class' constructor list too -> Can't do this without a return type really?
        accept(block) // TODO: The same as this
    }

}