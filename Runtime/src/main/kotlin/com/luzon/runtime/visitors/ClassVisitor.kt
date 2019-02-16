package com.luzon.runtime.visitors

import com.luzon.rd.ast.ASTNode
import com.luzon.rd.ast.ASTNodeVisitor
import com.luzon.rd.ast.accept
import com.luzon.runtime.SymbolTable
import com.luzon.runtime.nullObject

// Resolving & Binding?
object ClassVisitor : ASTNodeVisitor<Unit> {
    private fun accept(node: ASTNode?) = node?.accept(this) ?: nullObject // TODO: GlobalVisitor?

    override fun visit(node: ASTNode.Class) {
        SymbolTable.insert(node.name, node) // TODO: Scope

        accept(node.constructor)
        accept(node.block)
    }

}