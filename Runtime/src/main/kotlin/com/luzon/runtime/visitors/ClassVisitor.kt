package com.luzon.runtime.visitors

import com.luzon.rd.ast.ASTNode
import com.luzon.rd.expression.ASTNodeVisitor
import com.luzon.rd.expression.accept
import com.luzon.runtime.*

object ClassVisitor : ASTNodeVisitor<Any> {
    private fun accept(node: ASTNode?) {
        if (node is ASTNode.Expression) node.accept(ExpressionVisitor)
        else node?.accept(this)
    }

    override fun visit(node: ASTNode.Class) {
        val (name, constructor, block) = node

        val constructorFunction =
                if (constructor != null) visit(constructor)
                else LzFunction(name, emptyList(), null)

        ClassReferenceTable.classMap += name to LzClass(name, constructorFunction, processClassFunctions(block),
                EnvironmentManager.currentEnvironment, block)
    }

    override fun visit(node: ASTNode.ConstructorVariableDeclaration): ASTNode.FunctionParameter {
        TODO()
    }

    override fun visit(node: ASTNode.Constructor) =
            LzFunction("", node.variables.map { visit(it) }, null)

    // TODO: Return Type? Maybe make it a String rather than LzType?
    override fun visit(node: ASTNode.FunctionDefinition) =
            LzFunction(node.name, node.parameters, null, node.block)

    // TODO: Secondary Constructors
    private fun processClassFunctions(node: ASTNode.Block) = node.nodes.filter { it is ASTNode.FunctionDefinition }.map {
        visit(it as ASTNode.FunctionDefinition)
    }

    // TODO: The problem with this implementation, is that they may use functions from this class or others before they are added.
    private fun processClassVariables(block: ASTNode.Block): Environment {
        EnvironmentManager.newEnvironment()

        block.nodes.filter { it is ASTNode.VariableDeclaration }.forEach {
            it as ASTNode.VariableDeclaration
            EnvironmentManager += it.name to it.expr.accept(ExpressionVisitor)
        }

        return EnvironmentManager.pop()
    }
}