package com.luzon.runtime.visitors

import com.luzon.recursive_descent.ast.SyntaxTreeNode
import com.luzon.recursive_descent.expression.ASTNodeVisitor
import com.luzon.recursive_descent.expression.accept
import com.luzon.runtime.EnvironmentManager
import com.luzon.runtime.Return
import com.luzon.runtime.primitiveObject
import com.luzon.runtime.withNewEnvironment

object RuntimeVisitor : ASTNodeVisitor<Unit> {
    override fun visit(node: SyntaxTreeNode.VariableDeclaration) {
        val (name, _, expr, _) = node

        EnvironmentManager += name to expr.accept(ExpressionVisitor)
    }

    override fun visit(node: SyntaxTreeNode.VariableAssign) {
        val (name, expr) = node

        EnvironmentManager[name] = expr.accept(ExpressionVisitor)
    }

    override fun visit(node: SyntaxTreeNode.ForLoop) {
        (node.start..node.end).forEach {
            // This just boxes the loop in an extra environment
            withNewEnvironment {
                EnvironmentManager += node.id to primitiveObject(it)

                visit(node.block)
            }
        }
    }

    override fun visit(node: SyntaxTreeNode.WhileLoop) {
        if (node.doWhile) do {
            node.block.accept(this)
        } while (node.expr.accept(ExpressionVisitor).value == true)
        else while (node.expr.accept(ExpressionVisitor).value == true)
            node.block.accept(this)
    }

    override fun visit(node: SyntaxTreeNode.IfStatement) {
        val exprObj = node.expr.accept(ExpressionVisitor)

        if (exprObj.value == true) node.block.accept(this)
        else node.elseStatement?.accept(this)
    }

    override fun visit(node: SyntaxTreeNode.ElseStatements.ElseIfStatement) {
        node.ifStatement.accept(this)
    }

    override fun visit(node: SyntaxTreeNode.ElseStatements.ElseStatement) {
        node.block.accept(this)
    }

    @Throws(Return::class)
    override fun visit(node: SyntaxTreeNode.Block) {
        EnvironmentManager.newEnvironment()

        try {
            node.nodes.forEach {
                when (it) {
                    is SyntaxTreeNode.Expression -> it.accept(ExpressionVisitor)
                    else -> it.accept(this)
                }
            }
        } catch (ret: Return) {
            throw ret
        } finally {
            EnvironmentManager.pop()
        }
    }

    @Throws(Return::class)
    override fun visit(node: SyntaxTreeNode.Return) = throw Return(node.data?.accept(ExpressionVisitor))
}
