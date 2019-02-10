package com.luzon.rd.ast

interface ASTNodeVisitor<T> {
    fun visit(node: ASTNode.Class): T
    fun visit(node: ASTNode.Constructor): T
    fun visit(node: ASTNode.ConstructorVariableDeclaration): T
    fun visit(node: ASTNode.FunctionDefinition): T
    fun visit(node: ASTNode.FunctionParameter): T
    fun visit(node: ASTNode.ForLoop): T
    fun visit(node: ASTNode.WhileLoop): T
    fun visit(node: ASTNode.IfStatement): T
    fun visit(node: ASTNode.ElseStatements.ElseIfStatement): T
    fun visit(node: ASTNode.ElseStatements.ElseStatement): T
    fun visit(node: ASTNode.VariableDeclaration): T
    fun visit(node: ASTNode.VariableAssign): T
    fun visit(node: ASTNode.OperatorVariableAssign): T
    fun visit(node: ASTNode.Block): T
    fun visit(node: ASTNode.Expression.Binary.Plus): T
    fun visit(node: ASTNode.Expression.Binary.Sub): T
    fun visit(node: ASTNode.Expression.Binary.Mult): T
    fun visit(node: ASTNode.Expression.Binary.Div): T
    fun visit(node: ASTNode.Expression.Binary.Equals): T
    fun visit(node: ASTNode.Expression.Binary.NotEquals): T
    fun visit(node: ASTNode.Expression.Binary.GreaterEquals): T
    fun visit(node: ASTNode.Expression.Binary.Greater): T
    fun visit(node: ASTNode.Expression.Binary.Less): T
    fun visit(node: ASTNode.Expression.Binary.LessEquals): T
    fun visit(node: ASTNode.Expression.Binary.And): T
    fun visit(node: ASTNode.Expression.Binary.Or): T
    fun visit(node: ASTNode.Expression.Unary.Sub): T
    fun visit(node: ASTNode.Expression.Unary.Not): T
    fun visit(node: ASTNode.Expression.Unary.Increment): T
    fun visit(node: ASTNode.Expression.Unary.Decrement): T
    fun visit(node: ASTNode.Expression.LiteralExpr.IntLiteral): T
    fun visit(node: ASTNode.Expression.LiteralExpr.FloatLiteral): T
    fun visit(node: ASTNode.Expression.LiteralExpr.DoubleLiteral): T
    fun visit(node: ASTNode.Expression.LiteralExpr.IdentifierLiteral): T
    fun visit(node: ASTNode.Expression.LiteralExpr.FunctionCall): T
}

fun <T> ASTNode.accept(visitor: ASTNodeVisitor<T>) = when (this) {
    is ASTNode.Class -> visitor.visit(this)
    is ASTNode.Constructor -> visitor.visit(this)
    is ASTNode.ConstructorVariableDeclaration -> visitor.visit(this)
    is ASTNode.FunctionDefinition -> visitor.visit(this)
    is ASTNode.FunctionParameter -> visitor.visit(this)
    is ASTNode.ForLoop -> visitor.visit(this)
    is ASTNode.WhileLoop -> visitor.visit(this)
    is ASTNode.IfStatement -> visitor.visit(this)
    is ASTNode.ElseStatements.ElseIfStatement -> visitor.visit(this)
    is ASTNode.ElseStatements.ElseStatement -> visitor.visit(this)
    is ASTNode.VariableDeclaration -> visitor.visit(this)
    is ASTNode.VariableAssign -> visitor.visit(this)
    is ASTNode.OperatorVariableAssign -> visitor.visit(this)
    is ASTNode.Block -> visitor.visit(this)
    is ASTNode.Expression.Binary.Plus -> visitor.visit(this)
    is ASTNode.Expression.Binary.Sub -> visitor.visit(this)
    is ASTNode.Expression.Binary.Mult -> visitor.visit(this)
    is ASTNode.Expression.Binary.Div -> visitor.visit(this)
    is ASTNode.Expression.Binary.Equals -> visitor.visit(this)
    is ASTNode.Expression.Binary.NotEquals -> visitor.visit(this)
    is ASTNode.Expression.Binary.GreaterEquals -> visitor.visit(this)
    is ASTNode.Expression.Binary.Greater -> visitor.visit(this)
    is ASTNode.Expression.Binary.Less -> visitor.visit(this)
    is ASTNode.Expression.Binary.LessEquals -> visitor.visit(this)
    is ASTNode.Expression.Binary.And -> visitor.visit(this)
    is ASTNode.Expression.Binary.Or -> visitor.visit(this)
    is ASTNode.Expression.Unary.Sub -> visitor.visit(this)
    is ASTNode.Expression.Unary.Not -> visitor.visit(this)
    is ASTNode.Expression.Unary.Increment -> visitor.visit(this)
    is ASTNode.Expression.Unary.Decrement -> visitor.visit(this)
    is ASTNode.Expression.LiteralExpr.IntLiteral -> visitor.visit(this)
    is ASTNode.Expression.LiteralExpr.FloatLiteral -> visitor.visit(this)
    is ASTNode.Expression.LiteralExpr.DoubleLiteral -> visitor.visit(this)
    is ASTNode.Expression.LiteralExpr.IdentifierLiteral -> visitor.visit(this)
    is ASTNode.Expression.LiteralExpr.FunctionCall -> visitor.visit(this)
}