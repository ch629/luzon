package com.luzon.rd.ast

sealed class ASTNode {
    data class FunctionDefinition(val name: String, val parameters: List<FunctionParameter>, val returnType: String?, val block: Block) : ASTNode()
    data class FunctionParameter(val name: String, val type: String) : ASTNode()

    data class ForLoop(val id: String, val start: Int, val end: Int, val block: Block) : ASTNode() // TODO: Basic for loop for now.
    data class WhileLoop(val doWhile: Boolean, val expr: Expression, val block: Block) : ASTNode()

    data class IfStatement(val expr: Expression, val block: Block, val elseStatement: ElseStatements?) : ASTNode()

    sealed class ElseStatements : ASTNode() {
        data class ElseIfStatement(val ifStatement: IfStatement) : ElseStatements()
        data class ElseStatement(val block: Block) : ElseStatements()
    }

    data class VariableDeclaration(val name: String, val type: String?, val expr: Expression, val constant: Boolean) : ASTNode()
    data class VariableAssign(val name: String, val expr: Expression) : ASTNode()

    data class Block(val nodes: List<ASTNode>) : ASTNode()
}