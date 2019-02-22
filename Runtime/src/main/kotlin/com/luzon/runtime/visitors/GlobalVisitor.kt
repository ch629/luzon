package com.luzon.runtime.visitors

import com.luzon.rd.ast.ASTNode
import com.luzon.rd.expression.accept
import com.luzon.runtime.LzObject

object GlobalVisitor {
    fun visit(node: ASTNode): LzObject {

        if (node is ASTNode.Expression)
            node.accept(ExpressionVisitor)

        TODO()
    }
}