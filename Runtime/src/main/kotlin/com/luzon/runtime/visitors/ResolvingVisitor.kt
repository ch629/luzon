package com.luzon.runtime.visitors

import com.luzon.rd.ast.ASTNode
import com.luzon.rd.expression.ASTNodeVisitor
import com.luzon.rd.expression.accept
import com.luzon.runtime.ClassReferenceTable
import com.luzon.runtime.LzClass
import com.luzon.runtime.nullObject

// Resolving & Binding?
object ResolvingVisitor : ASTNodeVisitor<Unit> {
    private fun accept(node: ASTNode?) = node?.accept(this) ?: nullObject // TODO: GlobalVisitor?

    override fun visit(node: ASTNode.Class) {
        // TODO: Constructor to LzFunction
        ClassReferenceTable.classMap += node.name to
                LzClass(name = node.name, functions = emptyList(), block = node.block)

        val constructor = accept(node.constructor)
        val block = accept(node.block)
    }

}