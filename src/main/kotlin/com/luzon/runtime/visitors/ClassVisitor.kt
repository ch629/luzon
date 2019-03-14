package com.luzon.runtime.visitors

import com.luzon.rd.ast.ASTNode
import com.luzon.rd.expression.ASTNodeVisitor
import com.luzon.runtime.*

object ClassVisitor : ASTNodeVisitor<Any> {
    override fun visit(node: ASTNode.Class) {
        val (name, constructor, block) = node

        val constructorFunction =
                if (constructor != null) visit(constructor)
                else LzFunction(name, emptyList(), null)

        ClassReferenceTable += LzClass(name, constructorFunction, processClassFunctions(block),
                EnvironmentManager.currentEnvironment, block)
    }

    override fun visit(node: ASTNode.ConstructorVariableDeclaration) =
            ASTNode.FunctionParameter(node.name, node.type)

    override fun visit(node: ASTNode.Constructor) =
            LzCodeFunction("", node.variables.map { visit(it) }, null)

    // TODO: Return Type? Maybe make it a String rather than LzType?
    override fun visit(node: ASTNode.FunctionDefinition) =
            LzFunction(node.name, node.parameters, null, node.block)

    // TODO: Secondary Constructors
    private fun processClassFunctions(node: ASTNode.Block) = node.nodes.filter { it is ASTNode.FunctionDefinition }.map {
        visit(it as ASTNode.FunctionDefinition)
    }
}