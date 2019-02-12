package com.luzon.runtime

import com.luzon.rd.ast.ASTNode

data class LzFunction(val name: String, val params: List<ASTNode.FunctionParameter>, val returnType: LzType<*>?, val block: ASTNode.Block) {
    fun invoke(args: List<LzObject>): LzObject {
        TODO()
    }
}