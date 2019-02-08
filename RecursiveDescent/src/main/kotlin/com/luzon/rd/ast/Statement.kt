package com.luzon.rd.ast

import com.luzon.rd.ast.ASTNode.Expression

sealed class Statement {
    // TODO: Var/Val?, Visibility
    data class VariableDeclaration(val visibility: Visibility = Visibility.DEFAULT, val name: String, val type: String?, val value: Expression?) : Statement()

    data class VariableAssign(val visibility: Visibility = Visibility.DEFAULT, val name: String, val value: Expression?) : Statement()

    data class FunctionDefinition(val name: String, val returnType: String?, val block: ASTNode.Block) : Statement() // TODO: Parameters

    data class If(val expression: Expression, val elseStatement: Else?, val block: ASTNode.Block) : Statement()
    data class Else(val ifStatement: If?, val block: ASTNode.Block)

    data class Return(val expression: Expression?) : Statement()
}

enum class Visibility {
    DEFAULT, PUBLIC, PRIVATE, PROTECTED
}