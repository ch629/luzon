package com.luzon.rd.ast

sealed class ASTNode {
    data class FunctionDefinition(val name: String, val returnType: String?) : ASTNode()
    data class FunctionParameter(val name: String, val type: String) : ASTNode()

    data class VariableDeclaration(val name: String, val type: String?, val expr: Expression, val constant: Boolean) : ASTNode()
}