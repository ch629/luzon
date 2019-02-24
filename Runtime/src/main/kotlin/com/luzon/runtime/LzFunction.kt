package com.luzon.runtime

import com.luzon.rd.ast.ASTNode
import com.luzon.rd.expression.accept
import com.luzon.runtime.visitors.RuntimeVisitor

data class LzFunction(val name: String, val params: List<ASTNode.FunctionParameter>, val returnType: LzType<*>?,
                      val block: ASTNode.Block?, val onRun: (List<LzObject>) -> Unit = { block?.accept(RuntimeVisitor) }) {
    fun invoke(args: List<LzObject>): LzObject {
        onRun(args) // TODO: Could make return throw an exception, and catch it here

        return nullObject
    }
}