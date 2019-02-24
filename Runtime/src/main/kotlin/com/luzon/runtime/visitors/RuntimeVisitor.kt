package com.luzon.runtime.visitors

import com.luzon.rd.ast.ASTNode
import com.luzon.rd.expression.ASTNodeVisitor
import com.luzon.rd.expression.accept
import com.luzon.runtime.EnvironmentManager

object RuntimeVisitor : ASTNodeVisitor<Unit> {
//    private fun accept(node: ASTNode?) = node?.accept(this)

    override fun visit(node: ASTNode.VariableDeclaration) {
        val (name, type, expr, constant) = node

        EnvironmentManager += name to expr.accept(ExpressionVisitor)
    }

    override fun visit(node: ASTNode.VariableAssign) {
        val (name, expr) = node

        EnvironmentManager[name] = expr.accept(ExpressionVisitor)
    }


    override fun visit(node: ASTNode.ForLoop) {
        TODO()
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

    override fun visit(node: ASTNode.Block) {
        EnvironmentManager.newEnvironment()

        node.nodes.forEach {
            when (it) {
                is ASTNode.Expression -> it.accept(ExpressionVisitor)
                else -> it.accept(this)
            }
        }

        EnvironmentManager.pop()
    }
}