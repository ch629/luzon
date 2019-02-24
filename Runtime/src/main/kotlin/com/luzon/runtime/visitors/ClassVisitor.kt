package com.luzon.runtime.visitors

import com.luzon.rd.ast.ASTNode
import com.luzon.rd.expression.ASTNodeVisitor
import com.luzon.rd.expression.accept
import com.luzon.runtime.ClassReferenceTable
import com.luzon.runtime.LzClass
import com.luzon.runtime.LzFunction

object ClassVisitor : ASTNodeVisitor<Any> {
    private fun accept(node: ASTNode?) {
        if (node is ASTNode.Expression) node.accept(ExpressionVisitor)
        else node?.accept(this)
    }

    override fun visit(node: ASTNode.Class) {
        val (name, constructor, block) = node

        val constructorFunction =
                if (constructor != null) visit(constructor)
                else LzFunction(name, emptyList(), null, null) {
                    // TODO: Instantiate Class
                } // TODO: Return type should be the class

        // TODO: I just need to look at the function definitions, to add them to the class

        ClassReferenceTable.classMap += name to LzClass(name, constructorFunction, visit(block), block) // TODO: Functions?
    }

    // TODO: Block instantiate class
    // TODO: This also adds class variables if it contains val/var
    override fun visit(node: ASTNode.Constructor) =
            LzFunction("", node.variables.map { visit(it) }, null, ASTNode.Block(emptyList()))

    override fun visit(node: ASTNode.ConstructorVariableDeclaration): ASTNode.FunctionParameter {
        TODO()
    }

    // TODO: Secondary Constructors
    override fun visit(node: ASTNode.Block) = node.nodes.filter { it is ASTNode.FunctionDefinition }.map {
        visit(it as ASTNode.FunctionDefinition)
    }

    // TODO: Return Type? Maybe make it a String rather than LzType?
    override fun visit(node: ASTNode.FunctionDefinition) =
            LzFunction(node.name, node.parameters, null, node.block)
}