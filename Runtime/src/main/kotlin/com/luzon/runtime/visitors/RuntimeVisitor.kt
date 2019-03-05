package com.luzon.runtime.visitors

import com.luzon.rd.ast.ASTNode
import com.luzon.rd.expression.ASTNodeVisitor
import com.luzon.rd.expression.accept
import com.luzon.runtime.EnvironmentManager
import com.luzon.runtime.Return
import com.luzon.runtime.primitiveObject

object RuntimeVisitor : ASTNodeVisitor<Unit> {
    override fun visit(node: ASTNode.VariableDeclaration) {
        val (name, type, expr, constant) = node

        EnvironmentManager += name to expr.accept(ExpressionVisitor)
    }

    override fun visit(node: ASTNode.VariableAssign) {
        val (name, expr) = node

        EnvironmentManager[name] = expr.accept(ExpressionVisitor)
    }

    override fun visit(node: ASTNode.ForLoop) {
        (node.start..node.end).forEach {
            EnvironmentManager.newEnvironment()
            EnvironmentManager += node.id to primitiveObject(it) // This just boxes the loop in an extra environment

            visit(node.block)
            EnvironmentManager.pop()
        }
    }

    override fun visit(node: ASTNode.WhileLoop) {
        if (node.doWhile) do {
            node.block.accept(this)
        } while (node.expr.accept(ExpressionVisitor).value == true)
        else while (node.expr.accept(ExpressionVisitor).value == true)
            node.block.accept(this)
    }

    override fun visit(node: ASTNode.IfStatement) {
        val exprObj = node.expr.accept(ExpressionVisitor)

        if (exprObj.value == true) node.block.accept(this)
        else node.elseStatement?.accept(this)
    }

    override fun visit(node: ASTNode.ElseStatements.ElseIfStatement) {
        node.ifStatement.accept(this)
    }

    override fun visit(node: ASTNode.ElseStatements.ElseStatement) {
        node.block.accept(this)
    }

    @Throws(Return::class)
    override fun visit(node: ASTNode.Block) {
        EnvironmentManager.newEnvironment()

        node.nodes.forEach {
            try {
                when (it) {
                    is ASTNode.Expression -> it.accept(ExpressionVisitor)
                    else -> it.accept(this)
                }
            } catch (ret: Return) {
                EnvironmentManager.pop()
                throw ret
            }
        }

        EnvironmentManager.pop()
    }

    @Throws(Return::class)
    override fun visit(node: ASTNode.Return) = throw Return(node.data?.accept(ExpressionVisitor))
}