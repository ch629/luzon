package com.luzon.runtime.visitors

import com.luzon.recursive_descent.ast.SyntaxTreeNode
import com.luzon.recursive_descent.expression.ASTNodeVisitor
import com.luzon.runtime.ClassReferenceTable
import com.luzon.runtime.EnvironmentManager
import com.luzon.runtime.LzClass
import com.luzon.runtime.LzCodeFunction
import com.luzon.runtime.LzFunction

object ClassVisitor : ASTNodeVisitor<Any> {
    override fun visit(node: SyntaxTreeNode.Class) {
        val (name, constructor, block) = node

        val constructorFunction =
            if (constructor != null) visit(constructor)
            else LzFunction(name, emptyList(), null)

        ClassReferenceTable += LzClass(name, constructorFunction, processClassFunctions(block),
            EnvironmentManager.currentEnvironment, block)
    }

    override fun visit(node: SyntaxTreeNode.ConstructorVariableDeclaration) =
        SyntaxTreeNode.FunctionParameter(node.name, node.type)

    override fun visit(node: SyntaxTreeNode.Constructor) =
        LzCodeFunction("", node.variables.map { visit(it) }, null)

    // TODO: Return Type? Maybe make it a String rather than LzType?
    override fun visit(node: SyntaxTreeNode.FunctionDefinition) =
        LzFunction(node.name, node.parameters, null, node.block)

    // TODO: Secondary Constructors
    private fun processClassFunctions(node: SyntaxTreeNode.Block) = node.nodes.filterIsInstance<SyntaxTreeNode.FunctionDefinition>().map {
        visit(it)
    }
}
